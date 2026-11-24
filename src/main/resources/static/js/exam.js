/**
 * ExamEase - Interactive Examination Interface Script
 * Features: Local state management, Auto-saving, Countdown timer, Palette navigation, Auto-submit
 */

document.addEventListener("DOMContentLoaded", function () {
    const totalQuestions = parseInt(document.getElementById("totalQuestions").value, 10);
    const attemptId = document.getElementById("attemptId").value;
    let currentQuestion = 1;
    let isSubmitting = false;

    // Countdown Timer logic
    let remainingSeconds = parseInt(document.getElementById("initialRemainingSeconds").value, 10);
    const timerDisplay = document.getElementById("timerDisplay");
    const timerBox = document.getElementById("timerBox");

    function formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return (mins < 10 ? "0" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs;
    }

    function updateTimer() {
        if (remainingSeconds <= 0) {
            timerDisplay.textContent = "00:00";
            clearInterval(timerInterval);
            autoSubmitOnTimeout();
            return;
        }

        remainingSeconds--;
        timerDisplay.textContent = formatTime(remainingSeconds);

        if (remainingSeconds <= 300) { // 5 minutes or less
            timerBox.classList.add("urgent");
        }
    }

    const timerInterval = setInterval(updateTimer, 1000);
    timerDisplay.textContent = formatTime(remainingSeconds);

    function autoSubmitOnTimeout() {
        isSubmitting = true;
        alert("Time has expired! Your examination will now be submitted automatically.");
        document.getElementById("examForm").submit();
    }

    // Question Navigation & Display
    function showQuestion(qNum) {
        if (qNum < 1 || qNum > totalQuestions) return;

        // Hide all question items
        document.querySelectorAll(".question-block").forEach(block => {
            block.style.display = "none";
        });

        // Show active question
        const activeBlock = document.getElementById("question-block-" + qNum);
        if (activeBlock) {
            activeBlock.style.display = "block";
        }

        currentQuestion = qNum;

        // Update Previous / Next button states
        const prevBtn = document.getElementById("prevBtn");
        const nextBtn = document.getElementById("nextBtn");

        if (prevBtn) {
            prevBtn.disabled = (currentQuestion === 1);
            prevBtn.style.opacity = (currentQuestion === 1) ? "0.5" : "1";
        }
        if (nextBtn) {
            if (currentQuestion === totalQuestions) {
                nextBtn.textContent = "Review / Finish";
            } else {
                nextBtn.textContent = "Next Question →";
            }
        }

        updatePaletteHighlight();
    }

    // Palette highlight & status updater
    function updatePaletteHighlight() {
        document.querySelectorAll(".palette-btn").forEach(btn => {
            const num = parseInt(btn.getAttribute("data-qnum"), 10);
            if (num === currentQuestion) {
                btn.classList.add("active");
            } else {
                btn.classList.remove("active");
            }

            const qId = btn.getAttribute("data-qid");
            const selectedOption = document.querySelector(`input[name="answers[${qId}]"]:checked`);
            const isReview = document.getElementById(`review-check-${num}`) ? document.getElementById(`review-check-${num}`).checked : false;

            btn.classList.remove("answered", "review");

            if (selectedOption && selectedOption.value) {
                btn.classList.add("answered");
            }
            if (isReview) {
                btn.classList.add("review");
            }
        });
    }

    // Save answer asynchronously via background endpoint
    function saveAnswerToServer(questionId, answer, isReview) {
        fetch("/student/exam/save-answer", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                attemptId: attemptId,
                questionId: questionId,
                answer: answer,
                markForReview: isReview
            })
        }).catch(err => {
            console.warn("Background auto-save temporary sync issue:", err);
        });
    }

    // Option selection listener
    document.querySelectorAll(".option-radio").forEach(radio => {
        radio.addEventListener("change", function () {
            const qId = this.getAttribute("data-qid");
            const qNum = this.getAttribute("data-qnum");
            const selectedVal = this.value;

            // Update UI option labels
            document.querySelectorAll(`.option-label-${qNum}`).forEach(lbl => lbl.classList.remove("selected"));
            this.closest(".option-label").classList.add("selected");

            const isReview = document.getElementById(`review-check-${qNum}`).checked;
            updatePaletteHighlight();
            saveAnswerToServer(qId, selectedVal, isReview);
        });
    });

    // Mark for review checkbox listener
    document.querySelectorAll(".review-checkbox").forEach(check => {
        check.addEventListener("change", function () {
            const qNum = this.getAttribute("data-qnum");
            const qId = this.getAttribute("data-qid");
            const selectedRadio = document.querySelector(`input[name="answers[${qId}]"]:checked`);
            const selectedVal = selectedRadio ? selectedRadio.value : "";

            updatePaletteHighlight();
            saveAnswerToServer(qId, selectedVal, this.checked);
        });
    });

    // Clear answer button
    document.querySelectorAll(".clear-answer-btn").forEach(btn => {
        btn.addEventListener("click", function () {
            const qNum = this.getAttribute("data-qnum");
            const qId = this.getAttribute("data-qid");

            document.querySelectorAll(`input[name="answers[${qId}]"]`).forEach(radio => {
                radio.checked = false;
            });
            document.querySelectorAll(`.option-label-${qNum}`).forEach(lbl => lbl.classList.remove("selected"));

            const isReview = document.getElementById(`review-check-${qNum}`).checked;
            updatePaletteHighlight();
            saveAnswerToServer(qId, "", isReview);
        });
    });

    // Navigation buttons
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");

    if (prevBtn) {
        prevBtn.addEventListener("click", function () {
            if (currentQuestion > 1) {
                showQuestion(currentQuestion - 1);
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            if (currentQuestion < totalQuestions) {
                showQuestion(currentQuestion + 1);
            } else {
                openSubmitModal();
            }
        });
    }

    // Palette button clicks
    document.querySelectorAll(".palette-btn").forEach(btn => {
        btn.addEventListener("click", function () {
            const targetQNum = parseInt(this.getAttribute("data-qnum"), 10);
            showQuestion(targetQNum);
        });
    });

    // Submit Modal management
    const submitModal = document.getElementById("submitModal");
    const openModalBtn = document.getElementById("openSubmitModalBtn");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const confirmSubmitBtn = document.getElementById("confirmSubmitBtn");

    function openSubmitModal() {
        let answered = 0;
        let reviewCount = 0;

        document.querySelectorAll(".palette-btn").forEach(btn => {
            const qId = btn.getAttribute("data-qid");
            const num = btn.getAttribute("data-qnum");
            const selectedOption = document.querySelector(`input[name="answers[${qId}]"]:checked`);
            const isReview = document.getElementById(`review-check-${num}`) ? document.getElementById(`review-check-${num}`).checked : false;

            if (selectedOption && selectedOption.value) answered++;
            if (isReview) reviewCount++;
        });

        document.getElementById("modalTotalQ").textContent = totalQuestions;
        document.getElementById("modalAnsweredQ").textContent = answered;
        document.getElementById("modalReviewQ").textContent = reviewCount;
        document.getElementById("modalUnansweredQ").textContent = (totalQuestions - answered);

        submitModal.classList.add("active");
    }

    if (openModalBtn) {
        openModalBtn.addEventListener("click", openSubmitModal);
    }
    if (closeModalBtn) {
        closeModalBtn.addEventListener("click", function () {
            submitModal.classList.remove("active");
        });
    }
    if (confirmSubmitBtn) {
        confirmSubmitBtn.addEventListener("click", function () {
            isSubmitting = true;
            document.getElementById("examForm").submit();
        });
    }

    // Warn on reload or close during examination
    window.addEventListener("beforeunload", function (e) {
        if (!isSubmitting) {
            e.preventDefault();
            e.returnValue = "Are you sure you want to leave? Your examination answers are automatically saved, but the timer continues running.";
        }
    });

    // Initialize display with question 1
    showQuestion(1);
});
