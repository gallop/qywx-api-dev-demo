import {post} from '@/utils/ajax'


export function getAuthToken(data) {
  console.log("调用了api接口--getAuthToken")
  //getTokenTest  getWxToken
  return post('/qywx/getWxToken',data,false)
}
