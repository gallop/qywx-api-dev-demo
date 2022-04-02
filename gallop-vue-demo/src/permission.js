import router from './router'
import { getToken } from '@/utils/cookie' // getToken from cookie
import cas from '@utils/sso'
import Vue from 'vue'


const whiteList = ['/login', '/ssoLogin', '/401']// no redirect whitelist

router.beforeEach((to, from, next) => {
  console.log("from-url="+from.path)
  console.log("to-url="+to.path)
  console.log("next-name="+next.name)
  if (getToken()) { // determine if there has token
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      next()
    }
  } else {
    /* has no token*/
    if (whiteList.indexOf(to.path) !== -1) { // 在免登录白名单，直接进入
      next()
    } else {
      console.log('no token!!!')
      //next({ path: '/401', replace: true, query: { noGoBack: true }}) // 重定向到sso 登入处理页面
      //next({ path: '/ssoLogin' })
      var currentPath = to.path
      console.log("currentPath="+currentPath)
      //如果不是回调带code的地址，则存储to.path，以便获取token，跳转到这个地址
      if(to.path.indexOf("/callback") == -1){
        Vue.ls.set("beforeAuthUrl",to.path)
      }
      console.log("111111-beforeAuthUrl="+Vue.ls.get("beforeAuthUrl"))
      cas.enableCasAuth().then(()=>{
        //next()
        if(currentPath.indexOf("/callback") != -1){
          console.log("22222-beforeAuthUrl=="+Vue.ls.get("beforeAuthUrl"))
          next({path: Vue.ls.get("beforeAuthUrl")})
        }else {
          console.log(">>>>>>>>>>>>>>>>>>>need auth!!!!!!")
          //beforeAuthUrl = currentPath
        }
      })

    }
  }
})

router.afterEach(() => {

})
