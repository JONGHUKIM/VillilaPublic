// 배포시에만 콘솔 무시
if (location.hostname !== "localhost") {
  console.log = console.warn = console.error = () => {};
}