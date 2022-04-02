本demo对应的博客说明地址：[企业微信代开发流程](https://blog.mygallop.cn/2022/04/develop/qywx-api-dev/)



### demo代码的说明

#### 1 前端代码demo 正式环境的打包配置

1、在vue.config.js文件配置正式环境中域名目录的路径

````
module.exports = {
  publicPath: IS_PROD ? '/pm/' : '/', // 公共路径,默认'/'，部署应用包时的基本 URL
  outputDir: process.env.outputDir || 'dist', // 'dist', 将构建好的文件输出到哪里
````

>/pm/ 为正式环境中域名的子目录，配置此路径，所有的js，css文件才能找到正确的路径。

2、.env.production 文件配置后端api调用的地址：

````
VUE_APP_API = '/qywx-api'
````

> 此处不采用具体的api服务地点，而采用相对路径，在nginx 配置文件中配置路由转发规则，跳转到具体的后端api地址去，也避免跨域的问题。

nginx 配置文件wx.conf如下：

````
server {
    listen 443 ssl;
    server_name wx.mygallop.cn;

    ssl_certificate /etc/nginx/conf.d/ssl/wx.mygallop.cn_bundle.crt;
    ssl_certificate_key /etc/nginx/conf.d/ssl/wx.mygallop.cn.key;
    ssl_session_timeout 50m;
    ssl_protocols TLSv1 TLSv1.1 TLSv1.2; #请按照这个协议配置
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;#请按照这个套件配置
    ssl_prefer_server_ciphers on;

    charset utf-8;
    #access_log  /var/log/nginx/host.access.log  main;
    root /opt/webapp;
    location /pm {
        index index.html index.htm;
        #try_files $uri /index.html last;
        try_files $uri $uri/ /pm/index.html last;
    }
    #location /WW_verify_IWlMaiDFDH1xnpvC.txt {
    #    alias /opt/webapp/WW_verify_IWlMaiDFDH1xnpvC.txt;
    #}
    location /qywx {
        proxy_pass http://172.17.0.12:9096;
        proxy_set_header        Host $host;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_connect_timeout   90;
        proxy_send_timeout      90;
        proxy_read_timeout      90;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;#获取代理者的真实ip
        proxy_redirect          off;
    }
    location /qywx-api/ {
        proxy_pass http://172.17.0.12:9096/;
        proxy_set_header        Host $host;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # redirect server error pages to the static page /50x.html
    #
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }

}
server {
    listen 80;
    server_name wx.mygallop.cn; #填写绑定证书的域名
    #location / {
    #   root /opt/webapp;
    #}
    charset utf-8;
    location / {
        rewrite ^(.*)$ https://$host$1 permanent; #把http的域名请求转成https
    }
    location ~ .*\.(gif|jpg|png|jpeg|bmp|mp3|mp4)$ {
         rewrite ^(.*)$ https://$host$1 permanent; #把http的域名请求转成https
    }

}

````

> 注意 location /qywx-api/ 中proxy_pass地址配置的斜杠，“http://172.17.0.12:9096/”最后加了斜杠，这样跳转请求url时，会过滤掉location 后面的配置路径，即  
>
> 前端请求：http://wx.mygallop.cn/pm/qywx-api/qywx/getWxToken
>
> nginx 路由转发url： http://172.17.0.12:9096/qywx/getWxToken



最后，前端代码打包完上传服务器路径：/opt/webapp/pm 目录

#### 2 后端代码说明

1、处理oauth2 获取code 请求重定向，由前端发起请求授权code，后端组装好企业微信相关api，并进行重定向请求。

代码路径：com.gallop.wechat.controller.QywxLoginController 中的redirectAuthorize 方法；

2、进行企业微信api 调用（获取token、用户信息等等） 和 回调地址接口的开发暴露；

3、使用@Cacheable 和 @CacheEvict 对企业微信api返回的token信息进行redis 缓存；

4、由于部分回调地址对企业微信后台触发的相应需要在1000毫秒内（企业微信api文档明确要求），故部分信息要先放队列，后续进行处理，这里使用了redis的队列，注意配置在RedisConfig里，队列消息消费处理在 com.gallop.wechat.service.RedisReceiver 进行处理；