<script setup></script>

<template>
  <div class="otherWayLoginBox">
    <span>其它方式登录</span>
    <div class="otherWayLogin">
      <el-link :underline="false" class="el-link">微信登录</el-link>
      <el-link :underline="false" class="el-link">微博登录</el-link>
      <el-link :underline="false" class="el-link">QQ登录</el-link>
    </div>
  </div>
</template>

<style scoped>
.otherWayLoginBox {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.otherWayLoginBox span {
  font-size: 14px;
  color: #a8abb2;
  /* 防止用户多次点击触发文本，影响体验 */
  user-select: none;
  margin: 10px 0px;
}
.otherWayLoginBox .otherWayLogin {
  display: flex;
  width: 100%;
  margin: 10px 0 35px 0px;
}
.otherWayLoginBox .otherWayLogin .el-link {
  width: 88.4px;
  box-sizing: border-box;
  user-select: none;
}
.otherWayLoginBox .otherWayLogin .el-link:nth-child(1)::before {
  content: '';
  display: inline-block;
  width: 25px;
  height: 25px;
  background: url('@/assets/wechat.png') no-repeat;
  background-size: 100%;
  margin-right: 3px;
}
.otherWayLoginBox .otherWayLogin .el-link:nth-child(2)::before {
  content: '';
  display: inline-block;
  width: 28px;
  height: 28px;
  background: url('@/assets/MicroBlog.png') no-repeat;
  background-size: 100%;
  margin: 0 2px;
}
.otherWayLoginBox .otherWayLogin .el-link:nth-child(3)::before {
  content: '';
  display: inline-block;
  width: 28px;
  height: 28px;
  background: url('@/assets/qq.png') no-repeat;
  background-size: 100%;
  margin-right: 4px;
}
</style>
