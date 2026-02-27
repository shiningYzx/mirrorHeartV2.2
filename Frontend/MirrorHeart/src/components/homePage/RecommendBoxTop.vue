<scrip setup></scrip>
<template>
  <div class="recommendBoxTop">
    <slot name="title"></slot>
    <slot name="icon"></slot>
    <slot name="like"></slot>
  </div>
</template>
<style scoped>
.recommendBoxTop {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 40px;
}

.recommendBoxTop p {
  font-size: 25px;
  color: #000000;
  line-height: 40px;
  margin: 0 10px;
}

.recommendBoxTop img {
  width: 40px;
  height: 40px;
  margin: 0 10px;
}
</style>
