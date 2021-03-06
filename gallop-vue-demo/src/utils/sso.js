import {getToken, setToken} from '@/utils/cookie'
import {getAuthToken} from "@api/ssoAuth";
//import store from '../store'
//import router from '../router'
//https://wx.mygallop.cn
const casServerUrl = 'https://wx.mygallop.cn'
//https://wx.mygallop.cn/pm
const sysDomain = 'http://localhost:8088'
export default {
  async enableCasAuth() {
    console.log("into the enableCasAuth------")
    const token = getToken()
    if (token) {
      return
    }
    if (this.isSsoCallback()) {
      const code = this.getCode()
      console.log("before call getAuthToken------")
      /*const res = async() => {
        console.log("==========in in ==========")
        const data = await getAuthToken({code:code})
        return data
      }
      console.log("result-entity:"+ JSON.stringify(res))
      console.log("result-token:"+res.data.loginToken)
      setToken(res.data.loginToken)*/
      await getAuthToken({code:code}).then(res => {
        console.log("result-entity:"+ JSON.stringify(res.data))
        console.log("result-token:"+res.data.loginToken)
        setToken(res.data.loginToken)
        /*if (res.success) {
          const user_id = res.username.__text
          localStorage.setItem('name', user_id)
          this.login(user_id)
        } else {
          this.toAuth()
        }*/
      })
      return
    } else {
      console.log("to call get code ....")
      this.toAuth()
    }
  },
  toAuth() {
    //redirectAuth redirectTest
    location.href = `${casServerUrl}/qywx/redirectAuth`
  },
  getCode() {
    var reg = new RegExp('(^|&)code=([^&]*)(&|$)', 'i')
    var r = window.location.search.substr(1).match(reg)
    if (r != null) return unescape(r[2])
    return null
  },
  isSsoCallback() {
    const url = window.location.href
    return url.indexOf('/callback?code=') != -1
  },
  logout() {
    location.href = `${casServerUrl}/logout?service=${sysDomain}`
  },
}
