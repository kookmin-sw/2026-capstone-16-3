const sections = document.querySelectorAll("section[id]");
const navLinks = document.querySelectorAll('.greedy-nav a[href^="#"]');

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;

      const id = entry.target.getAttribute("id");

      navLinks.forEach((link) => {
        link.classList.remove("active");

        if (link.getAttribute("href") === `#${id}`) {
          link.classList.add("active");
        }
      });
    });
  },
  {
    rootMargin: "-40% 0px -50% 0px",
    threshold: 0
  }
);

sections.forEach((section) => {
  observer.observe(section);
});