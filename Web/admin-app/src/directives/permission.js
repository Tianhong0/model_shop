import { useAuthStore } from '@/stores/auth'

/**
 * v-permission 指令
 *
 * 使用方式:
 * v-permission="'user:create'"           // 单个权限
 * v-permission="['user:create', 'user:edit']"  // 任一权限
 * v-permission:all="['user:create', 'user:edit']"  // 全部权限
 */
export const permissionDirective = {
  mounted(el, binding) {
    const { value, arg } = binding
    const authStore = useAuthStore()

    // 未登录无权限
    if (!authStore.isAuthenticated) {
      el.parentNode?.removeChild(el)
      return
    }

    // 超级管理员拥有所有权限
    if (authStore.user?.roles?.includes('ROLE_ADMIN')) {
      return
    }

    // 空权限视为通过
    if (!value) {
      return
    }

    const permissions = Array.isArray(value) ? value : [value]
    const mode = arg === 'all' ? 'all' : 'any'

    let hasPermission = false
    if (mode === 'all') {
      hasPermission = permissions.every(p => authStore.permissions.includes(p))
    } else {
      hasPermission = permissions.some(p => authStore.permissions.includes(p))
    }

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * v-role 指令
 *
 * 使用方式:
 * v-role="'admin'"           // 单个角色
 * v-role="['admin', 'editor']"  // 任一角色
 */
export const roleDirective = {
  mounted(el, binding) {
    const { value } = binding
    const authStore = useAuthStore()

    if (!authStore.isAuthenticated) {
      el.parentNode?.removeChild(el)
      return
    }

    const roles = Array.isArray(value) ? value : [value]
    const hasRole = roles.some(role =>
      authStore.user?.roles?.includes(`ROLE_${role.toUpperCase()}`) ||
      authStore.user?.roles?.includes(role)
    )

    if (!hasRole) {
      el.parentNode?.removeChild(el)
    }
  }
}
