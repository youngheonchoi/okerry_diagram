const form = document.querySelector(".repository-form");
const repositoryUrlInput = document.querySelector("#repository-url");
const analyzeButton = document.querySelector("#analyze-button");
const formHelp = document.querySelector("#form-help");

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    analyzeButton.disabled = true;
    formHelp.textContent = "Cloning the repository…";

    try {
        const response = await fetch("/api/projects/analyze", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({repositoryUrl: repositoryUrlInput.value})
        });
        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || "Repository clone failed.");
        }

        formHelp.textContent = "Repository clone completed. Source analysis will be added in Phase 3.";
    } catch (error) {
        formHelp.textContent = error.message;
    } finally {
        analyzeButton.disabled = false;
    }
});
