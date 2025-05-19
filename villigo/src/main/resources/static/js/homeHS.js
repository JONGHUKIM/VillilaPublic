document.addEventListener('DOMContentLoaded', function() {
    const newProducts = document.getElementById("newProducts");
    const categoryBtns = document.querySelectorAll(".category-btn, .scroll-arrow");
    const scrollArrowBtns = document.querySelectorAll(".scroll-arrow");
    const brandData = document.getElementById("brandData");
    const bagbrands = brandData.dataset.bagbrands;
    const carbrands = brandData.dataset.carbrands;
    
    
    categoryBtns.forEach(categoryBtn => {
        categoryBtn.addEventListener('click', function() {
            console.log("카테고리 클릭함!");
        });
    });
    
    scrollArrowBtns.forEach(scrollArrowBtn => {
        scrollArrowBtn.addEventListener('click', function() {
        console.log("스크롤 클릭함!");
        });
    });
});