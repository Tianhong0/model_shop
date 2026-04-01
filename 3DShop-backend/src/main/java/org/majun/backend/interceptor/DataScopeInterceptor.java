package org.majun.backend.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.majun.backend.annotation.DataScope;
import org.majun.backend.annotation.DataScopes;
import org.majun.backend.context.DataScopeContext;
import org.majun.backend.enums.DataScopeType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * 数据权限拦截器
 * 自动为SELECT语句添加数据范围过滤条件
 */
@Slf4j
@Component
public class DataScopeInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {

        // 检查是否跳过数据权限检查
        if (DataScopeContext.isSkipCheck()) {
            return;
        }

        // 获取数据权限信息
        DataScopeContext.DataScopeInfo scopeInfo = DataScopeContext.get();
        if (scopeInfo == null) {
            return;
        }

        // 如果是全部数据权限，跳过
        if (scopeInfo.getScopeType() == DataScopeType.ALL) {
            return;
        }

        // 获取注解信息
        DataScope dataScope = getDataScopeAnnotation(ms);
        if (dataScope == null || !dataScope.enabled()) {
            return;
        }

        // 管理员跳过检查
        if (dataScope.adminSkip() && scopeInfo.isSkipCheck()) {
            return;
        }

        // 获取当前用户ID
        Long userId = scopeInfo.getUserId();
        if (userId == null) {
            return;
        }

        // 构建数据权限SQL条件
        String ownerField = dataScope.ownerField();
        String tableAlias = dataScope.tableAlias();

        // 解析SQL并添加条件
        try {
            String originalSql = boundSql.getSql();
            Select select = (Select) CCJSqlParserUtil.parse(originalSql);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

            Expression where = plainSelect.getWhere();
            Expression dataScopeCondition = buildDataScopeCondition(tableAlias, ownerField, userId);

            if (where == null) {
                plainSelect.setWhere(dataScopeCondition);
            } else {
                plainSelect.setWhere(new AndExpression(where, dataScopeCondition));
            }

            // 更新SQL - 使用反射修改BoundSql中的sql字段
            String newSql = select.toString();
            try {
                java.lang.reflect.Field field = boundSql.getClass().getDeclaredField("sql");
                field.setAccessible(true);
                field.set(boundSql, newSql);
            } catch (NoSuchFieldException e) {
                // 如果无法直接修改，尝试通过其他方式
                log.debug("无法直接修改SQL，尝试备用方案");
            }

        } catch (Exception e) {
            log.warn("数据权限SQL解析失败: {}", e.getMessage());
        }
    }

    /**
     * 构建数据权限条件表达式
     */
    private Expression buildDataScopeCondition(String tableAlias, String ownerField, Long userId) {
        String columnName = (tableAlias != null && !tableAlias.isEmpty())
                ? tableAlias + "." + ownerField
                : ownerField;

        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(columnName));
        equalsTo.setRightExpression(new LongValue(userId));

        return equalsTo;
    }

    /**
     * 获取方法上的DataScope注解
     */
    private DataScope getDataScopeAnnotation(MappedStatement ms) {
        try {
            String id = ms.getId();
            String className = id.substring(0, id.lastIndexOf("."));
            String methodName = id.substring(id.lastIndexOf(".") + 1);

            Class<?> clazz = Class.forName(className);
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    // 先检查DataScopes注解
                    DataScopes dataScopes = method.getAnnotation(DataScopes.class);
                    if (dataScopes != null && dataScopes.value().length > 0) {
                        return dataScopes.value()[0];
                    }
                    // 检查DataScope注解
                    return method.getAnnotation(DataScope.class);
                }
            }
        } catch (Exception e) {
            log.debug("获取DataScope注解失败: {}", e.getMessage());
        }
        return null;
    }
}
