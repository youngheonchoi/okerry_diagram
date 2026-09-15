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

        formHelp.textContent = `Analysis completed: ${result.controllerCount} controllers, ${result.apiCount} APIs.`;
        await loadControllers(result.projectId);
    } catch (error) {
        formHelp.textContent = error.message;
    } finally {
        analyzeButton.disabled = false;
    }
});

async function loadControllers(projectId) {
    const response = await fetch(`/api/projects/${projectId}/controllers`);
    const controllers = await response.json();
    const controllerResults = document.querySelector("#controller-results");
    const controllerList = document.querySelector("#controller-list");

    controllerList.replaceChildren(...controllers.map((controller) => {
        const section = document.createElement("section");
        const title = document.createElement("h3");
        const apiList = document.createElement("ul");

        title.textContent = controller.className;
        controller.apis.forEach((api) => {
            const item = document.createElement("li");
            item.textContent = `${api.httpMethod} ${api.requestPath} — ${api.methodName}()`;
            apiList.append(item);
        });
        section.append(title, apiList);
        return section;
    }));
    controllerResults.hidden = false;
}
