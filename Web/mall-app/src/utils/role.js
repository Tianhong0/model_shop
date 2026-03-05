const normalizeRoleText = (rawRole) => String(rawRole || '').trim().toUpperCase()

export const isDesignerRole = (rawRole) => {
  if (Array.isArray(rawRole)) {
    return rawRole.some(item => isDesignerRole(item))
  }
  if (rawRole && typeof rawRole === 'object') {
    return isDesignerRole(
      rawRole.roleName || rawRole.authority || rawRole.name || rawRole.code || rawRole.role
    )
  }
  const role = normalizeRoleText(rawRole)
  return role === 'DESIGNER' || role === 'ROLE_DESIGNER'
}

export const normalizeUserRole = (rawRole) => {
  return isDesignerRole(rawRole) ? 'designer' : 'user'
}

export const getStoredUserRole = () => {
  const storedRole = uni.getStorageSync('user_role')
  const profile = uni.getStorageSync('user_profile') || {}
  return normalizeUserRole(storedRole || profile.role)
}
