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

  async function loadSemesters(programmeId, select, selectedId = "", placeholder = "All semesters") {
    if (!programmeId) { fillSelect(select, [], placeholder); return; }
    fillSelect(select, await loadOptions(`/api/catalog/programmes/${programmeId}/semesters`), placeholder, selectedId);
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
    });
    semesterFilter?.addEventListener("change", async () => {
      if (!isSignedIn) {
        if (semesterFilter.value) localStorage.setItem(preferenceSemester, semesterFilter.value);
        else localStorage.removeItem(preferenceSemester);
      }
    });
  }

  document.querySelectorAll("[data-subject-suggestions]").forEach(input => {
    const list = document.getElementById(input.dataset.suggestionList);
    const form = input.form;
    const hidden = input.dataset.subjectTarget ? document.getElementById(input.dataset.subjectTarget) : null;
    const programme = input.dataset.programmeSource ? document.getElementById(input.dataset.programmeSource) : null;
    const semester = input.dataset.semesterSource ? document.getElementById(input.dataset.semesterSource) : null;
    const status = document.getElementById(`${list?.id.replace("-suggestions", "")}-status`);
    if (!list || !form) return;

    let matches = [];
    let activeIndex = -1;
    let requestNumber = 0;
    let timer;
    let selectedText = hidden?.value ? input.value : "";
    const close = () => {
      list.hidden = true;
      input.setAttribute("aria-expanded", "false");
      input.removeAttribute("aria-activedescendant");
      activeIndex = -1;
    };
    const open = () => {
      list.hidden = false;
      input.setAttribute("aria-expanded", "true");
    };
    const showNoMatches = message => {
      list.replaceChildren();
      const option = document.createElement("div");
      option.className = "suggestion-empty";
      option.setAttribute("role", "option");
      option.setAttribute("aria-disabled", "true");
      option.textContent = message;
      list.append(option);
      open();
    };
    const setActive = index => {
      if (!matches.length) return;
      activeIndex = (index + matches.length) % matches.length;
      list.querySelectorAll('[role="option"][data-index]').forEach(option => {
        const active = Number(option.dataset.index) === activeIndex;
        option.setAttribute("aria-selected", String(active));
      });
      input.setAttribute("aria-activedescendant", `${list.id}-option-${activeIndex}`);
    };
    const choose = subject => {
      input.value = subject.name;
      selectedText = subject.name;
      if (hidden) hidden.value = String(subject.id);
      if (status) status.textContent = "";
      close();
      if (input.dataset.subjectMode === "filter") form.requestSubmit();
    };
    const loadSuggestions = async () => {
      const term = input.value.trim();
      if (hidden && term !== selectedText) hidden.value = "";
      if (status) status.textContent = "";
      if (term.length < 2) { close(); return; }
      const currentRequest = ++requestNumber;
      const params = new URLSearchParams({ q: term });
      if (programme?.value) params.set("programmeId", programme.value);
      if (semester?.value) params.set("semesterId", semester.value);
      try {
        const response = await fetch(`/api/catalog/subjects/suggestions?${params}`, { headers: { Accept: "application/json" } });
        if (!response.ok) throw new Error("Subject suggestions unavailable");
        const results = await response.json();
        if (currentRequest !== requestNumber) return;
        matches = results;
        list.replaceChildren();
        if (!matches.length) {
          showNoMatches("No matching subjects. Press Enter to search resources for this keyword.");
          return;
        }
        matches.forEach((subject, index) => {
          const option = document.createElement("button");
          option.type = "button";
          option.id = `${list.id}-option-${index}`;
          option.className = "subject-suggestion";
          option.setAttribute("role", "option");
          option.setAttribute("aria-selected", "false");
          option.dataset.index = String(index);
          const name = document.createElement("span");
          name.className = "subject-suggestion-name";
          name.textContent = subject.name;
          const details = document.createElement("span");
          details.className = "subject-suggestion-details";
          details.textContent = `${subject.programme} · ${subject.semester}${subject.code ? ` · ${subject.code}` : ""}`;
          option.append(name, details);
          option.addEventListener("mousedown", event => event.preventDefault());
          option.addEventListener("click", () => choose(subject));
          list.append(option);
        });
        activeIndex = -1;
        open();
      } catch {
        if (currentRequest !== requestNumber) return;
        matches = [];
        close();
        if (status) status.textContent = "Subject suggestions could not load. You can still search using this keyword.";
      }
    };
    const scheduleSuggestions = () => {
      clearTimeout(timer);
      requestNumber++;
      matches = [];
      close();
      timer = setTimeout(loadSuggestions, 180);
    };

    input.addEventListener("input", () => {
      if (hidden && input.value !== selectedText) {
        hidden.value = "";
        selectedText = "";
      }
      scheduleSuggestions();
    });
    input.addEventListener("keydown", event => {
      if (event.key === "ArrowDown" && !list.hidden && matches.length) {
        event.preventDefault(); setActive(activeIndex + 1);
      } else if (event.key === "ArrowUp" && !list.hidden && matches.length) {
        event.preventDefault(); setActive(activeIndex < 0 ? matches.length - 1 : activeIndex - 1);
      } else if (event.key === "Enter" && !list.hidden && activeIndex >= 0 && matches[activeIndex]) {
        event.preventDefault(); choose(matches[activeIndex]);
      } else if (event.key === "Escape" && !list.hidden) {
        event.preventDefault(); close();
      }
    });
    input.addEventListener("blur", () => setTimeout(close, 100));
    [programme, semester].filter(Boolean).forEach(select => select.addEventListener("change", () => {
      requestNumber++;
      clearTimeout(timer);
      if (hidden) hidden.value = "";
      selectedText = "";
      matches = [];
      close();
    }));
  });

  const profileProgramme = document.getElementById("profile-programme");
  const profileSemester = document.getElementById("profile-semester");
  profileProgramme?.addEventListener("change", () => loadSemesters(profileProgramme.value, profileSemester));

  const registrationProgramme = document.getElementById("registration-programme");
  const registrationSemester = document.getElementById("registration-semester");
  const registrationForm = registrationProgramme?.form;
  const registrationProgrammeError = document.getElementById("registration-programme-error");
  const registrationSemesterError = document.getElementById("registration-semester-error");
  const registrationSemesterHelp = document.getElementById("registration-semester-help");
  let registrationLoadNumber = 0;
  function clearRegistrationErrors() {
    registrationProgramme?.setCustomValidity("");
    registrationSemester?.setCustomValidity("");
    if (registrationProgrammeError) registrationProgrammeError.textContent = "";
    if (registrationSemesterError) registrationSemesterError.textContent = "";
    registrationProgramme?.removeAttribute("aria-invalid");
    registrationSemester?.removeAttribute("aria-invalid");
  }
  async function updateRegistrationSemesters(preserveSelected = false) {
    if (!registrationProgramme || !registrationSemester) return;
    const loadNumber = ++registrationLoadNumber;
    const programmeId = registrationProgramme.value;
    const priorSemester = preserveSelected ? registrationSemester.value : "";
    registrationSemester.disabled = true;
    if (!programmeId) {
      fillSelect(registrationSemester, [], "Choose a programme first");
      registrationSemester.disabled = !priorSemester;
      if (registrationSemesterHelp) {
        registrationSemesterHelp.textContent = priorSemester
          ? "Choose the programme that matches this semester, or clear the semester."
          : "Select a programme to load its available semesters.";
      }
      return;
    }
    fillSelect(registrationSemester, [], "Loading semesters…");
    if (registrationSemesterHelp) registrationSemesterHelp.textContent = "Loading semesters for the selected programme…";
    try {
      const items = await loadOptions(`/api/catalog/programmes/${programmeId}/semesters`);
      if (loadNumber !== registrationLoadNumber) return;
      fillSelect(registrationSemester, items, items.length ? "Choose semester (optional)" : "No semesters available", priorSemester);
      registrationSemester.disabled = items.length === 0;
      if (registrationSemesterHelp) {
        registrationSemesterHelp.textContent = items.length
          ? "Choose a semester, or leave both academic details blank."
          : "This programme has no semesters available. Choose another programme or leave both blank.";
      }
    } catch {
      if (loadNumber !== registrationLoadNumber) return;
      fillSelect(registrationSemester, [], "Could not load semesters");
      registrationSemester.disabled = true;
      if (registrationSemesterHelp) registrationSemesterHelp.textContent = "Could not load semesters. Check your connection and choose the programme again.";
    }
  }
  if (registrationProgramme && registrationSemester) {
    if (registrationProgramme.value) {
      updateRegistrationSemesters(true);
    } else if (!registrationSemester.value) {
      registrationSemester.disabled = true;
    }
    registrationProgramme.addEventListener("change", () => {
      clearRegistrationErrors();
      updateRegistrationSemesters(false);
    });
    registrationSemester.addEventListener("change", clearRegistrationErrors);
    registrationForm?.addEventListener("submit", event => {
      clearRegistrationErrors();
      if (registrationProgramme.value && !registrationSemester.value) {
        const message = "Choose a current semester for this programme, or clear the programme.";
        if (registrationSemesterError) registrationSemesterError.textContent = message;
        registrationSemester?.setAttribute("aria-invalid", "true");
        registrationSemester?.setCustomValidity(message);
        event.preventDefault();
        if (!registrationSemester.disabled) registrationSemester.reportValidity();
        else registrationProgramme.focus();
      } else if (!registrationProgramme.value && registrationSemester.value) {
        const message = "Choose the programme that this semester belongs to.";
        if (registrationProgrammeError) registrationProgrammeError.textContent = message;
        registrationProgramme?.setAttribute("aria-invalid", "true");
        registrationProgramme?.setCustomValidity(message);
        event.preventDefault();
        registrationProgramme.reportValidity();
      }
    });
  }

  const uploadProgramme = document.getElementById("upload-programme");
  const uploadSemester = document.getElementById("upload-semester");
  if (uploadProgramme && uploadProgramme.value) {
    const selectedSemester = uploadSemester.value;
    loadSemesters(uploadProgramme.value, uploadSemester, selectedSemester, "Choose a semester").catch(() => {});
  }
  uploadProgramme?.addEventListener("change", () => loadSemesters(uploadProgramme.value, uploadSemester, "", "Choose a semester"));

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
