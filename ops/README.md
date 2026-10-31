# oip-web-backend deployables

This folder contains ops/runtime assets for the oip-web-backend service.

- `ops/helm/`: Helm chart for Kubernetes deployments (dev/preprod/prod)

## Helm

Chart path: `ops/helm`

Install with defaults:

```bash
helm upgrade --install oip-web-backend \
  oip-web-backend/ops/helm \
  -n oip --create-namespace
```

Environment values:

- `environments/dev/values.yaml`
- `environments/preprod/values.yaml`
- `environments/prod/values.yaml`

Each environment file is self-contained and carries the chart defaults for that target environment.

Recommended deploy command:

```bash
helm upgrade --install oip-web-backend \
  oip-web-backend/ops/helm \
  -n oip --create-namespace \
  -f oip-web-backend/ops/helm/environments/preprod/values.yaml
```

Secrets policy:

- Never commit production secrets in values files.
- Inject `secrets.DB_PASSWORD` and `secrets.OIP_SECURITY_JWT_SECRET` at deploy time
  (`--set-string`) or from a cluster secret manager.

Schema validation:

- Validate before deploy:

```bash
helm lint oip-web-backend/ops/helm \
  -f oip-web-backend/ops/helm/environments/preprod/values.yaml \
  --set-string secrets.DB_PASSWORD=lint-only \
  --set-string secrets.OIP_SECURITY_JWT_SECRET=lint-only-lint-only-lint-only-1234
```

## Dev deployment

Use `make dev-up` from `oip-web-backend` to deploy the service to your local cluster.

```bash
cd oip-web-backend && make dev-check
cd oip-web-backend && make dev-up
```

Run locally:

```bash
cd oip-web-backend && make run-api
```

Stop:

```bash
cd oip-web-backend && make dev-down
```
