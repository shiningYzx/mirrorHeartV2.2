<script setup></script>
<template>
  <div class="logo">
    <img src="@/assets/logo.png" alt="Element logo" />
    <span>Mirror Heart</span>
  </div>
</template>
<style scoped>
/* 设置logo */
.logo {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.logo img {
  width: 35px;
  margin-right: 10px;
  margin-bottom: 5px;
}

.logo span {
  font-size: 18px;
  user-select: none;
  font-family: 'Pacifico', 'Dancing Script', cursive, sans-serif;
  font-weight: 500;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #a18cd1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
</style>
