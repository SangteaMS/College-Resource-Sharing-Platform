const resources = [
  {id:"java-basics",title:"Java basics: classes and objects",subject:"Java",semester:"Semester 1",type:"Notes",description:"A short introduction to classes, objects, fields, and methods.",text:"FICTIONAL SAMPLE RESOURCE\n\nJava basics: classes and objects\n\nA class is a blueprint. An object is one instance created from that blueprint.\n\nExample:\nclass Book {\n    String title;\n}\n\nBook notes = new Book();\nnotes.title = \"Java basics\";"},
  {id:"java-loops",title:"Loop practice worksheet",subject:"Java",semester:"Semester 1",type:"Practice",description:"Beginner exercises for for-loops and while-loops.",text:"FICTIONAL SAMPLE RESOURCE\n\nLoop practice\n\n1. Print the numbers from 1 to 10 with a for-loop.\n2. Add the numbers from 1 to 20 with a while-loop.\n3. Print a multiplication table for a number you choose."},
  {id:"dbms-keys",title:"Database keys quick guide",subject:"DBMS",semester:"Semester 2",type:"Study guide",description:"A compact guide to primary keys, foreign keys, and candidate keys.",text:"FICTIONAL SAMPLE RESOURCE\n\nA primary key uniquely identifies a row. A foreign key points to a key in another table. A candidate key is any minimal set of columns that can uniquely identify a row."},
  {id:"dbms-sql",title:"SQL query practice",subject:"DBMS",semester:"Semester 2",type:"Practice",description:"Practice SELECT, WHERE, and ORDER BY with a fictional student table.",text:"FICTIONAL SAMPLE RESOURCE\n\nGiven Student(student_id, name, semester), select all students in semester 2, ordered by name.\n\nExample answer:\nSELECT student_id, name FROM Student WHERE semester = 2 ORDER BY name;"},
  {id:"osad-git",title:"Git and GitHub starter notes",subject:"Open Source Application Development",semester:"Semester 3",type:"Notes",description:"A beginner-friendly summary of commits, branches, and pull requests.",text:"FICTIONAL SAMPLE RESOURCE\n\nA repository stores project history. A commit records a change. A branch lets you work on an idea separately. A pull request asks collaborators to review and combine your changes."},
  {id:"osad-license",title:"Open source licenses overview",subject:"Open Source Application Development",semester:"Semester 3",type:"Study guide",description:"An introduction to why open source projects include licenses.",text:"FICTIONAL SAMPLE RESOURCE\n\nA license tells other people how they may use, change, and share a project. Always check the license terms before reusing code."},
  {id:"r-summary",title:"R summary statistics example",subject:"Statistics with R",semester:"Semester 2",type:"R script",description:"A small R example that calculates and prints common summary values.",text:"# FICTIONAL SAMPLE RESOURCE: R summary statistics\nscores <- c(62, 74, 81, 69, 90)\nmean(scores)\nmedian(scores)\nsummary(scores)"},
  {id:"r-chart",title:"Make a simple bar chart in R",subject:"Statistics with R",semester:"Semester 2",type:"R script",description:"A tiny example showing how to create a bar chart from fictional counts.",text:"# FICTIONAL SAMPLE RESOURCE: simple bar chart\nsubjects <- c(\"Java\", \"DBMS\", \"R\")\ncounts <- c(12, 9, 7)\nbarplot(counts, names.arg = subjects, col = \"seagreen\")"}
];

const byId = id => document.getElementById(id);
const search = byId("search");
const subjectFilter = byId("subject-filter");
const semesterFilter = byId("semester-filter");
const typeFilter = byId("type-filter");

function fillOptions(select, values) {
  for (const value of [...new Set(values)].sort()) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = value;
    select.append(option);
  }
}

fillOptions(subjectFilter, resources.map(item => item.subject));
fillOptions(semesterFilter, resources.map(item => item.semester));
fillOptions(typeFilter, resources.map(item => item.type));

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"})[char]);
}

function renderResources() {
  const query = search.value.trim().toLowerCase();
  const matches = resources.filter(item =>
    (!query || `${item.title} ${item.subject} ${item.description}`.toLowerCase().includes(query)) &&
    (!subjectFilter.value || item.subject === subjectFilter.value) &&
    (!semesterFilter.value || item.semester === semesterFilter.value) &&
    (!typeFilter.value || item.type === typeFilter.value));

  byId("result-count").textContent = `${matches.length} sample ${matches.length === 1 ? "resource" : "resources"}`;
  const list = byId("resource-list");
  if (!matches.length) {
    list.innerHTML = '<div class="empty-state"><b>No matching resources yet.</b><br>Try another search or clear the filters.</div>';
    return;
  }
  list.innerHTML = matches.map(item => {
    const id = escapeHtml(item.id);
    const title = escapeHtml(item.title);
    const subject = escapeHtml(item.subject);
    const semester = escapeHtml(item.semester);
    const type = escapeHtml(item.type);
    const description = escapeHtml(item.description);
    const icon = item.type === "R script" ? "R" : item.subject === "DBMS" ? "DB" : "DOC";
    const color = item.type === "R script" ? "purple" : "green";
    return `<article class="resource-card-row"><span class="file-icon ${color}">${icon}</span><div class="resource-info"><p class="resource-meta">${subject} · ${semester} · ${type}</p><h3>${title}</h3><p class="resource-description">${description}</p></div><div class="resource-actions"><button class="outline-button detail-toggle" type="button" aria-expanded="false" aria-controls="detail-${id}">Details <span aria-hidden="true">⌄</span></button><button class="download-link" type="button" data-download="${id}">Download ↓</button></div><div class="resource-details" id="detail-${id}" hidden>${escapeHtml(item.text)}</div></article>`;
  }).join("");

  list.querySelectorAll(".detail-toggle").forEach(button => button.addEventListener("click", () => {
    const details = byId(button.getAttribute("aria-controls"));
    const open = details.hidden;
    details.hidden = !open;
    button.setAttribute("aria-expanded", String(open));
    button.innerHTML = open ? 'Hide details <span aria-hidden="true">⌃</span>' : 'Details <span aria-hidden="true">⌄</span>';
  }));
  list.querySelectorAll("[data-download]").forEach(button => button.addEventListener("click", () => {
    const item = resources.find(resource => resource.id === button.dataset.download);
    const file = new Blob([item.text], {type:"text/plain;charset=utf-8"});
    const link = document.createElement("a");
    link.href = URL.createObjectURL(file);
    link.download = `${item.id}${item.type === "R script" ? ".R" : ".txt"}`;
    link.click();
    URL.revokeObjectURL(link.href);
  }));
}

byId("filter-form").addEventListener("submit", event => { event.preventDefault(); renderResources(); });
search.addEventListener("input", renderResources);
for (const select of [subjectFilter, semesterFilter, typeFilter]) select.addEventListener("change", renderResources);
byId("clear-filters").addEventListener("click", () => {
  search.value = "";
  subjectFilter.value = "";
  semesterFilter.value = "";
  typeFilter.value = "";
  renderResources();
});
document.querySelectorAll("[data-subject-link]").forEach(link => link.addEventListener("click", () => {
  subjectFilter.value = link.dataset.subjectLink;
  renderResources();
}));

const requestedSubject = new URLSearchParams(location.search).get("subject");
if (requestedSubject && [...subjectFilter.options].some(option => option.value === requestedSubject)) subjectFilter.value = requestedSubject;
renderResources();
