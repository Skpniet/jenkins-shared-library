# Universal Jenkins Pipeline (Standalone)

This repository contains a standalone Jenkins pipeline implementation that runs entirely from ci/universalPipeline.groovy.

## Final Workflow

1. Developer pushes code to Git.
2. Jenkins job triggers on push/webhook.
3. Root Jenkinsfile loads ci/universalPipeline.groovy.
4. Branch validation is performed.
5. Code checkout happens inside an ephemeral Kubernetes pod.
6. Infrastructure scan runs with Checkov.
7. SBOM is generated with Syft.
8. Code quality scan runs with SonarQube.
9. Docker image is built in a Docker-in-Docker pod.
10. Image scan is performed with Trivy.
11. Image is pushed to the configured registry if REGISTRY_URL is provided.
12. Notification is sent after completion.

## Files

- Jenkinsfile - root pipeline loader that runs the standalone pipeline script.
- Jenkinsfile.standalone - example Jenkinsfile showing how to load the standalone script.
- ci/universalPipeline.groovy - standalone pipeline script with all workflow stages.
- ci/k8s/sonar-pod.yaml - ephemeral pod template for SonarQube stage.
- ci/k8s/checkov-pod.yaml - ephemeral pod template for Checkov stage.
- ci/k8s/sbom-pod.yaml - ephemeral pod template for SBOM generation.
- ci/k8s/docker-pod.yaml - ephemeral pod template for Docker build and image scan.

## Usage

1. Configure Jenkins Kubernetes plugin to use your EKS cluster.
2. Ensure the EKS pod service account (for example jenkins-build-sa) exists and has required IAM permissions.
3. Add the required Jenkins credentials:
   - sonar-token
   - deptrack-api-key
   - egistry-credentials
4. Use Jenkinsfile.standalone or the root Jenkinsfile to run the pipeline.

### Example



## Notes

- This repository no longer uses a Jenkins Shared Library.
- All pipeline logic is contained in ci/universalPipeline.groovy.
- The pipeline is configured to run per-stage ephemeral pods in EKS.
- If you use a registry, set REGISTRY_URL and provide egistry-credentials.

## Push to GitHub

This repo is already on GitHub. If you want to copy the standalone pipeline into a new application repo, copy the full ci/ folder and use Jenkinsfile.standalone.
