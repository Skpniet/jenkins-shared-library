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

### Ephemeral Pod (Kubernetes) Usage

This library now provides a reusable pipeline entry `universalPipeline` (in `vars/universalPipeline.groovy`) that runs using Jenkins Kubernetes plugin ephemeral pods. Example usage from a Jenkinsfile:

```groovy
@Library('jenkins-shared-library') _
universalPipeline(
   GIT_URL: 'https://github.com/your-org/your-repo.git',
   GIT_BRANCH: 'main',
   ENVIRONMENT: 'staging'
)
```

Requirements on the Jenkins side:
- Jenkins Kubernetes plugin installed and configured to talk to your EKS cluster.
- A `serviceAccount` in EKS (example: `jenkins-build-sa`) with needed IAM permissions.
- Credentials in Jenkins:
   - `sonar-token` (secret text)
   - `deptrack-api-key` (secret text)

See `k8s/podTemplate.yaml` for a sample Pod manifest for EKS agents.

### Pipeline Flow

The `universalPipeline` entry implements the following flow (matches your requested flow):

- Developer Push Code
- Jenkins Trigger
- Load Shared Library (this repo)
- Load GlobalConfig (from `jenkins-global-config` repo when configured)
- Environment Validation (reads `environments.yaml`)
- Branch Validation (regex-based)
- AWS Role Assumption (if `AWS_ROLE_ARN` provided)
- Code Checkout
- Security Scanning (Checkov, SBOM generation)
- Code Quality Scan (SonarQube)
- Docker Build
- Image Scan (Trivy)
- Push Artifact / Image (to configured registry)

Configure the following Jenkins credentials (recommended ids):
- `github-credentials` (username/password or token)
- `sonar-token` (secret text)
- `deptrack-api-key` (secret text)
- `aws-credentials` (AWS credentials)
- `registry-credentials` (username/password for registry)


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
