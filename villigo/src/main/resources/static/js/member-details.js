function showTab(index) {
  const tabs = document.querySelectorAll(".tab");
  const contents = document.querySelectorAll(".tab-content");
 
  tabs.forEach((tab, i) => { 
    tab.classList.toggle("active", i === index);
    contents[i].classList.toggle("active", i === index);
  });
}

document.addEventListener("DOMContentLoaded", function () {
  const tabs = document.querySelectorAll(".tab");
  const contents = document.querySelectorAll(".tab-content");

  tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => {
      showTab(index);
    });
  });
});


