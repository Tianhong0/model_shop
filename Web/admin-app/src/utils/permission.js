import { useAuthStore } from '@/stores/auth'

/**
 * 检查是否有指定权限
 * @param {string|string[]} permission 权限码或权限码数组
 * @param {string} mode 模式: 'any' 任一权限, 'all' 全部权限
 * @returns {boolean}
 */
export function checkPermission(permission, mode = 'any') {
  const authStore = useAuthStore()

  // 未登录无权限
  if (!authStore.isAuthenticated) return false

  // 超级管理员拥有所有权限
  if (authStore.user?.roles?.includes('ROLE_ADMIN')) return true

  // 空权限视为通过
  if (!permission) return true

  const permissions = Array.isArray(permission) ? permission : [permission]

  if (mode === 'all') {
    return permissions.every(p => authStore.permissions.includes(p))
  }

  return permissions.some(p => authStore.permissions.includes(p))
}

/**
 * 检查是否有任一权限
 * @param {string|string[]} permissions 权限码或权限码数组
 * @returns {boolean}
 */
export function hasAnyPermission(permissions) {
  return checkPermission(permissions, 'any')
}

/**
 * 检查是否有全部权限
 * @param {string[]} permissions 权限码数组
 * @returns {boolean}
 */
export function hasAllPermissions(permissions) {
  return checkPermission(permissions, 'all')
}

/**
 * 检查是否有指定角色
 * @param {string|string[]} roles 角色或角色数组
 * @returns {boolean}
 */
export function hasRole(roles) {
  const authStore = useAuthStore()

  if (!authStore.isAuthenticated) return false

  const roleList = Array.isArray(roles) ? roles : [roles]
  return roleList.some(role =>
    authStore.user?.roles?.includes(`ROLE_${role.toUpperCase()}`) ||
    authStore.user?.roles?.includes(role)
  )
}

/**
 * 过滤有权限的路由
 * @param {Array} routes 路由列表
 * @returns {Array} 过滤后的路由列表
 */
export function filterPermissionRoutes(routes) {
  return routes.filter(route => {
    const meta = route.meta || {}

    // 无权限要求
    if (!meta.permission) return true

    // 检查权限
    return checkPermission(meta.permission)
  })
}
