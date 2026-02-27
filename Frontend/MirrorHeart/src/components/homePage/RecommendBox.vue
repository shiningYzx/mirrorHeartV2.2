<script setup></script>
<template>
  <div class="recommendBox">
    <div class="recommendBoxTop">
      <slot name="title"></slot>
      <slot name="like"></slot>
      <slot name="icon"></slot>
    </div>
    <div class="recommendBoxBottom">
      <slot name="body"></slot>
    </div>
  </div>
</template>
<style>
.recommendBox {
  margin: 0 10px;
  border-radius: 8px;
  cursor: pointer;
  background-color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s;
  min-height: 320px;
  max-height: 320px;
  height: 320px;
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.recommendBox:hover {
  transform: scale(0.95);
  background: linear-gradient(#d6f7ff, #fff);
}

.recommendBoxTop {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 40px;
  padding-top: 20px;
}

.recommendBoxTop p {
  font-size: 25px;
  color: #000000;
  line-height: 40px;
  margin: 0 10px;
  font-family: 'SimSun', '宋体', 'Microsoft YaHei';
}

.recommendBoxTop img {
  width: 40px;
  height: 40px;
  margin: 0 10px;
}

.recommendBoxTop .like-button {
  cursor: pointer;
  margin: 0 10px;
  font-size: 20px;
  transition: transform 0.2s;
}

.recommendBoxTop .like-button:hover {
  transform: scale(1.2);
}

.recommendBoxBottom {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: calc(100% - 40px);
  padding: 0 20px 15px 20px;
  overflow: hidden;
}

.recommendBoxBottom img {
  width: 67%;
  box-shadow:
    0 4px 8px rgba(0, 0, 0, 0.2),
    /* 底部较大阴影 */ -4px 0 8px rgba(0, 0, 0, 0.1),
    /* 左侧较小阴影 */ 4px 0 8px rgba(0, 0, 0, 0.1); /* 右侧较小阴影 */
}

.book-brief {
  white-space: nowrap; /* 让文本在一行显示 */
  overflow: hidden; /* 隐藏溢出部分 */
  text-overflow: ellipsis; /* 显示省略号 */
  max-width: 100%; /*设置最大宽度，根据实际情况调整*/
  font-family: 'SimSun', '宋体', 'Microsoft YaHei';
}

.sentence-content {
  font-size: 22px;
  line-height: 40px;
  text-indent: 2em;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 6; /* 最多显示3行 */
  overflow: hidden;
  text-overflow: ellipsis;
  width: 92%;
}

.article-content {
  text-indent: 2em;
  font-size: 20px;
  line-height: 28px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 7; /* 最多显示3行 */
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
