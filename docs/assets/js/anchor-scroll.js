document.addEventListener(
  "click",
  function (event) {
    const link = event.target.closest('a[href*="#"]');
    if (!link) return;

    const href = link.getAttribute("href");
    if (!href || href === "#") return;

    const url = new URL(href, window.location.href);

    if (url.pathname !== window.location.pathname) return;

    const targetId = url.hash;
    if (!targetId) return;

    const target = document.querySelector(targetId);
    const masthead = document.querySelector(".masthead");

    if (!target || !masthead) return;

    event.preventDefault();
    event.stopPropagation();

    const navbarHeight = masthead.getBoundingClientRect().height;
    const extraOffset = 24;

    const targetY =
      target.getBoundingClientRect().top +
      window.pageYOffset -
      navbarHeight -
      extraOffset;

    window.scrollTo({
      top: targetY,
      behavior: "smooth"
    });

    history.pushState(null, "", url.pathname + targetId);
  },
  true
);