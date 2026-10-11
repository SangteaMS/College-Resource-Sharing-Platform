(() => {
  const preferenceCourse = "collegeHub.preferredProgrammeId";
  const preferenceSemester = "collegeHub.currentSemesterId";

  const navToggle = document.getElementById("nav-toggle");
  const mainNavigation = document.getElementById("main-navigation");
  function closeNavigation() {
    navToggle?.setAttribute("aria-expanded", "false");
    navToggle?.setAttribute("aria-label", "Open navigation");
    mainNavigation?.classList.remove("is-open");
  }
  navToggle?.addEventListener("click", () => {
    const isOpen = navToggle.getAttribute("aria-expanded") === "true";
    navToggle.setAttribute("aria-expanded", String(!isOpen));
    navToggle.setAttribute("aria-label", isOpen ? "Open navigation" : "Close navigation");
    mainNavigation?.classList.toggle("is-open", !isOpen);
  });
  mainNavigation?.querySelectorAll("a").forEach(link => link.addEventListener("click", closeNavigation));
  document.addEventListener("keydown", event => { if (event.key === "Escape") closeNavigation(); });

  async function loadOptions(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (!response.ok) throw new Error("Could not load course options");
    return response.json();
  }

  function fillSelect(select, items, placeholder, selectedId = "") {
    if (!select) return;
    select.replaceChildren(new Option(placeholder, ""));
    for (const item of items) {
      const option = new Option(item.label ?? `${item.name} (${item.code})`, String(item.id));
      if (item.number !== undefined) option.text = item.label;
      if (item.semesterId) option.dataset.semesterId = item.semesterId;
      select.add(option);
    }
    if (selectedId && [...select.options].some(option => option.value === String(selectedId))) {
      select.value = String(selectedId);
    }
  }

  async function loadSemesters(programmeId, select, selectedId = "") {
    if (!programmeId) { fillSelect(select, [], "All semesters"); return; }
    fillSelect(select, await loadOptions(`/api/catalog/programmes/${programmeId}/semesters`), "All semesters", selectedId);
  }

  async function loadSubjects(programmeId, semesterId, select, selectedId = "") {
    if (!programmeId) { fillSelect(select, [], "All subjects"); return; }
    if (!semesterId) {
      const all = await loadOptions(`/api/catalog/programmes/${programmeId}/subjects`);
      fillSelect(select, all, "All subjects", selectedId);
      return;
    }
    fillSelect(select, await loadOptions(`/api/catalog/semesters/${semesterId}/subjects`), "All subjects", selectedId);
  }

  const homeCourse = document.getElementById("home-programme");
  const homeSemester = document.getElementById("home-semester");
  const homeCourseForm = document.getElementById("home-course-form");
  const isSignedIn = document.body.dataset.signedIn === "true";
  if (homeCourse && !isSignedIn && !homeCourse.value) {
    const saved = localStorage.getItem(preferenceCourse);
    if (saved && [...homeCourse.options].some(option => option.value === saved)) homeCourse.value = saved;
  }
  if (homeCourse && homeSemester && homeCourse.value) {
    const selectedSemester = homeSemester.value || (!isSignedIn ? localStorage.getItem(preferenceSemester) : "") || "";
    loadSemesters(homeCourse.value, homeSemester, selectedSemester).catch(() => {});
  }
  if (homeCourse) {
    homeCourse.addEventListener("change", async () => {
      if (!isSignedIn) {
        localStorage.setItem(preferenceCourse, homeCourse.value);
        localStorage.removeItem(preferenceSemester);
      }
      await loadSemesters(homeCourse.value, homeSemester);
    });
  }
  if (homeSemester) homeSemester.addEventListener("change", () => {
    if (!isSignedIn) localStorage.setItem(preferenceSemester, homeSemester.value);
  });
  homeCourseForm?.addEventListener("submit", () => {
    let allCourses = homeCourseForm.querySelector('input[name="allCourses"]');
    if (!homeCourse?.value) {
      if (!allCourses) {
        allCourses = document.createElement("input");
        allCourses.type = "hidden";
        allCourses.name = "allCourses";
        homeCourseForm.append(allCourses);
      }
      allCourses.value = "true";
    } else {
      allCourses?.remove();
    }
  });
  document.getElementById("browse-all-courses")?.addEventListener("click", () => {
    window.location.assign("/resources?allCourses=true");
  });

  const programmeFilter = document.getElementById("filter-programme");
  const semesterFilter = document.getElementById("filter-semester");
  const subjectFilter = document.getElementById("filter-subject");
  const resourceForm = document.getElementById("resource-filter-form");
  const query = new URLSearchParams(window.location.search);
  if (resourceForm && !isSignedIn && !query.has("programmeId") && query.get("allCourses") !== "true") {
    const savedCourse = localStorage.getItem(preferenceCourse);
    if (savedCourse) {
      const redirect = new URL(window.location.href);
      redirect.searchParams.set("programmeId", savedCourse);
      const savedSemester = localStorage.getItem(preferenceSemester);
      if (savedSemester) redirect.searchParams.set("semesterId", savedSemester);
      window.location.replace(redirect.toString());
      return;
    }
  }
  if (programmeFilter) {
    programmeFilter.addEventListener("change", async () => {
      const programmeId = programmeFilter.value;
      if (!isSignedIn) {
        if (programmeId) localStorage.setItem(preferenceCourse, programmeId);
        else localStorage.removeItem(preferenceCourse);
        localStorage.removeItem(preferenceSemester);
      }
      await loadSemesters(programmeId, semesterFilter);
      await loadSubjects(programmeId, "", subjectFilter);
    });
    semesterFilter?.addEventListener("change", async () => {
      if (!isSignedIn) {
        if (semesterFilter.value) localStorage.setItem(preferenceSemester, semesterFilter.value);
        else localStorage.removeItem(preferenceSemester);
      }
      await loadSubjects(programmeFilter.value, semesterFilter.value, subjectFilter);
    });
    subjectFilter?.addEventListener("change", () => {
      const chosen = subjectFilter.selectedOptions[0];
      if (chosen?.dataset.semesterId && !semesterFilter.value) semesterFilter.value = chosen.dataset.semesterId;
      if (!isSignedIn && semesterFilter.value) localStorage.setItem(preferenceSemester, semesterFilter.value);
    });
  }

  const profileProgramme = document.getElementById("profile-programme");
  const profileSemester = document.getElementById("profile-semester");
  profileProgramme?.addEventListener("change", () => loadSemesters(profileProgramme.value, profileSemester));

  const registrationProgramme = document.getElementById("registration-programme");
  const registrationSemester = document.getElementById("registration-semester");
  if (registrationProgramme && registrationSemester && registrationProgramme.value) {
    const selected = registrationSemester.value;
    loadSemesters(registrationProgramme.value, registrationSemester, selected).catch(() => {});
  }
  registrationProgramme?.addEventListener("change", () => loadSemesters(registrationProgramme.value, registrationSemester));

  const uploadProgramme = document.getElementById("upload-programme");
  const uploadSemester = document.getElementById("upload-semester");
  const uploadSubject = document.getElementById("upload-subject");
  if (uploadProgramme && uploadProgramme.value) {
    const selectedSemester = uploadSemester.value;
    const selectedSubject = uploadSubject.value;
    loadSemesters(uploadProgramme.value, uploadSemester, selectedSemester)
      .then(() => loadSubjects(uploadProgramme.value, uploadSemester.value, uploadSubject, selectedSubject)).catch(() => {});
  }
  uploadProgramme?.addEventListener("change", async () => {
    await loadSemesters(uploadProgramme.value, uploadSemester);
    await loadSubjects(uploadProgramme.value, "", uploadSubject);
  });
  uploadSemester?.addEventListener("change", () => loadSubjects(uploadProgramme.value, uploadSemester.value, uploadSubject));

  const resourceType = document.getElementById("resource-type");
  const examFields = document.getElementById("exam-fields");
  const examType = document.getElementById("exam-type");
  const examSession = document.getElementById("exam-session");
  const examSessionLabel = document.getElementById("exam-session-label");
  function updateExamFields() {
    if (!examFields || !resourceType) return;
    const isPaper = resourceType.value === "QUESTION_PAPER";
    examFields.hidden = !isPaper;
    const isOther = examType?.value === "OTHER";
    if (examSession) examSession.required = Boolean(isPaper && isOther);
    if (examSessionLabel) examSessionLabel.firstChild.textContent = isOther ? "Describe the exam type " : "Optional exam session or, for Other, a description of the exam type ";
  }
  resourceType?.addEventListener("change", updateExamFields);
  examType?.addEventListener("change", updateExamFields);
  updateExamFields();

  const fileInput = document.getElementById("resource-file");
  const fileSelection = document.getElementById("file-selection");
  const dropArea = document.getElementById("file-drop");
  const maximumBytes = Number(dropArea?.dataset.maxBytes || 20971520);
  function displayFile(file) {
    if (!file) { if (fileSelection) fileSelection.textContent = "No new file selected."; return; }
    const maximumMegabytes = Math.max(1, Math.round(maximumBytes / 1024 / 1024));
    if (file.size > maximumBytes) {
      if (fileSelection) fileSelection.textContent = `This file is larger than the ${maximumMegabytes} MB limit. Choose a smaller file.`;
      if (fileInput) { fileInput.value = ""; fileInput.setCustomValidity(`Choose a file smaller than ${maximumMegabytes} MB.`); }
      return;
    }
    fileInput?.setCustomValidity("");
    if (fileSelection) fileSelection.textContent = `${file.name} · ${(file.size / 1024 / 1024).toFixed(2)} MB`;
  }
  fileInput?.addEventListener("change", () => displayFile(fileInput.files[0]));
  dropArea?.addEventListener("dragover", event => { event.preventDefault(); dropArea.classList.add("drag-over"); });
  dropArea?.addEventListener("dragleave", () => dropArea.classList.remove("drag-over"));
  dropArea?.addEventListener("drop", event => {
    event.preventDefault(); dropArea.classList.remove("drag-over");
    if (fileInput && event.dataTransfer.files.length) {
      fileInput.files = event.dataTransfer.files;
      displayFile(fileInput.files[0]);
    }
  });
  document.getElementById("resource-upload-form")?.addEventListener("submit", event => {
    const form = event.currentTarget;
    if (!form.reportValidity()) { event.preventDefault(); return; }
    const submit = document.getElementById("submit-upload");
    if (submit) { submit.disabled = true; submit.textContent = "Uploading and saving…"; }
  });
})();
