# Universal Jenkins Pipeline

This repository contains a reusable Jenkins pipeline for:

- Git checkout
- Static Application Security Testing (SAST) via SonarQube
- Infrastructure-as-Code (IaC) scanning via Checkov
- Software Composition Analysis (SCA) via Dependency-Track
- Notification on build result

## Files

- `Jenkinsfile` - main declarative pipeline
- `src/gitClone.groovy` - Git checkout helper
- `src/sonarqubeScanner.groovy` - SonarQube scan helper
- `src/checkovScanner.groovy` - Checkov scan helper
- `utils/dependencyTrack.groovy` - Dependency-Track BOM upload helper
- `utils/notify.groovy` - Notification helper

## Usage

1. Open Jenkins and create a new pipeline job.
2. Point the job to this repository or paste the `Jenkinsfile`.
3. Configure credentials / environment variables:
   - `SONAR_TOKEN` for SonarQube authentication
   - `DEPTRACK_API_KEY` for Dependency-Track API access
4. Provide `GIT_URL` and `GIT_BRANCH` parameters if needed.

## Notes

- The pipeline assumes `sonar-scanner`, `checkov`, `syft`, and `curl` are installed on the Jenkins agent.
- Update `DEPTRACK_URL` and notification email in the `Jenkinsfile` environment block.

## Push to GitHub

Because this environment does not have Git installed, initialize Git and push from your machine:

```bash
cd universal-jenkins-pipeline
git init
git add .
git commit -m "Add universal Jenkins pipeline"
git remote add origin https://github.com/<your-org>/<your-repo>.git
git push -u origin main
```
