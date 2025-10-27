let timeLeft = 60;
const timerDisplay = document.getElementById("time");
const params = new URLSearchParams(window.location.search);
const student = params.get("student");
let isSubmitted = false; // Flag to prevent double submission

/**
 * Handles all logic for submitting the exam.
 * Prevents double submissions.
 */
function submitExam() {
  if (isSubmitted) return; // Prevent running twice
  isSubmitted = true;
  clearInterval(timer); // Stop the timer

  const formData = new FormData(document.getElementById("examForm"));
  let score = 0;
  
  // Find total questions from the hidden inputs
  const hiddenInputs = document.querySelectorAll('input[type="hidden"]');
  const total = hiddenInputs.length;

  for (let [k, v] of formData.entries()) {
    if (k.startsWith("q")) {
      const id = k.substring(1);
      const correct = formData.get("correct" + id);
      if (v === correct) score++;
    }
  }

  fetch("/api/submit", {
    method: "POST",
    body: `student=${student}&score=${score}`
  });

  // Redirect to result page
  window.location.href = `result.html?student=${student}&score=${score}-out-of-${total}`;
}

// --- Timer Logic ---
const timer = setInterval(() => {
  timeLeft--;
  timerDisplay.textContent = timeLeft;
  if (timeLeft <= 0) {
    // UPDATED: Call the new function instead of .submit()
    submitExam();
  }
}, 1000);

// --- Question Loading ---
fetch("/api/questions")
  .then(res => res.json())
  .then(data => {
    const div = document.getElementById("questions");
    data.forEach((q, i) => {
      div.innerHTML += `
        <div class="question">
          <p>${i + 1}. ${q.question}</p>
          <label><input type="radio" name="q${q.id}" value="A"> ${q.A}</label>
          <label><input type="radio" name="q${q.id}" value="B"> ${q.B}</label>
          <label><input type="radio" name="q${q.id}" value="C"> ${q.C}</label>
          <input type="hidden" name="correct${q.id}" value="${q.correct}">
        </div>
      `;
    });
  });

// --- Form Submit Button Logic ---
document.getElementById("examForm").addEventListener("submit", e => {
  e.preventDefault(); // Still prevent default browser behavior
  submitExam(); // Call the new function
});