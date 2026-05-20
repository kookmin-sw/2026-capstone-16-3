const sections = document.querySelectorAll("section[id]");
const navLinks = document.querySelectorAll('.greedy-nav a[href*="#"]');

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;

      const id = entry.target.getAttribute("id");

      navLinks.forEach((link) => {
        link.classList.remove("active");

        const url = new URL(link.getAttribute("href"), window.location.href);

        if (url.hash === `#${id}`) {
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

document.addEventListener("DOMContentLoaded", () => {
  const modal = document.getElementById("guideImageModal");
  const modalImage = modal?.querySelector(".gilbeot-image-modal-img");
  const closeButton = modal?.querySelector(".gilbeot-image-modal-close");
  const guideImages = document.querySelectorAll(".gilbeot-guide-image");

  if (!modal || !modalImage || !closeButton || guideImages.length === 0) {
    return;
  }

  const openModal = (image) => {
    modalImage.src = image.currentSrc || image.src;
    modalImage.alt = image.alt || "확대된 가이드 이미지";

    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("gilbeot-modal-open");
  };

  const closeModal = () => {
    modal.classList.remove("is-open");
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("gilbeot-modal-open");

    modalImage.src = "";
    modalImage.alt = "";
  };

  guideImages.forEach((image) => {
    image.addEventListener("click", () => openModal(image));
  });

  closeButton.addEventListener("click", closeModal);

  modal.addEventListener("click", (event) => {
    if (event.target === modal) {
      closeModal();
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && modal.classList.contains("is-open")) {
      closeModal();
    }
  });
});