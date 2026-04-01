import { permissionDirective, roleDirective } from './permission'

/**
 * 注册全局自定义指令
 * @param {App} app Vue应用实例
 */
export function setupDirectives(app) {
  // 注册 v-permission 指令
  app.directive('permission', permissionDirective)

  // 注册 v-role 指令
  app.directive('role', roleDirective)
}

export { permissionDirective, roleDirective }
