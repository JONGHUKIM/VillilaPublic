document.addEventListener("DOMContentLoaded", function () {
    const mypageBtns = document.querySelectorAll(".mypage-btn");
    const mypagePopups = document.querySelectorAll(".mypage-popup");
    const alarmBtns = document.querySelectorAll(".alarm-btn");
    const alarmPopups = document.querySelectorAll(".alarm-popup");

    function closeAllPopups() {
        mypagePopups.forEach(popup => popup.classList.remove("show"));
        alarmPopups.forEach(popup => popup.style.display = "none");
    }

    mypageBtns.forEach((btn, index) => {
        const popup = mypagePopups[index];
        btn.addEventListener("click", function (e) {
            e.stopPropagation();
            const isOpen = popup.classList.contains("show");
            closeAllPopups();
            if (!isOpen) popup.classList.add("show");
        });
    });

    alarmBtns.forEach((btn, index) => {
        const popup = alarmPopups[index];
        btn.addEventListener("click", function (e) {
            e.stopPropagation();
            const isOpen = popup.style.display === "block";
            closeAllPopups();
            if (!isOpen) popup.style.display = "block";
        });
    });

    document.addEventListener("click", function (e) {
        const isClickInsideMypage = Array.from(mypageBtns).some(btn => btn.contains(e.target)) ||
                                    Array.from(mypagePopups).some(popup => popup.contains(e.target));
        const isClickInsideAlarm = Array.from(alarmBtns).some(btn => btn.contains(e.target)) ||
                                   Array.from(alarmPopups).some(popup => popup.contains(e.target));
        if (!isClickInsideMypage && !isClickInsideAlarm) {
            closeAllPopups();
        }
    });
});


// 헤더 스크롤 이벤트
document.addEventListener('DOMContentLoaded', function() {
  const headerTop = document.querySelector('.header-top');
  const headerBottom = document.querySelector('.header-bottom');
  
  // 초기 상태 설정
  headerBottom.style.opacity = '0';
  headerBottom.style.visibility = 'hidden';
  
  // 스크롤 이벤트 리스너 추가
  window.addEventListener('scroll', function() {
    const scrollPosition = window.scrollY;
    
    // 콘솔에 스크롤 위치 출력 (디버깅용)
    console.log('스크롤 위치:', scrollPosition);
    
    // 스크롤 위치가 50px 이상이면 헤더 변경
    if (scrollPosition > 130) {
      headerTop.style.opacity = '0';
      headerTop.style.visibility = 'hidden';
      
      headerBottom.style.opacity = '1';
      headerBottom.style.visibility = 'visible';
      headerBottom.classList.add('visible');
    } else {
      headerTop.style.opacity = '1';
      headerTop.style.visibility = 'visible';
      
      headerBottom.style.opacity = '0';
      headerBottom.style.visibility = 'hidden';
      headerBottom.classList.remove('visible');
    }
  });
});





