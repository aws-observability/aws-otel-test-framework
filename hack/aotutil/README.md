# aotutil

`aoutil` is go binary that implement some test logic that is hard to express/maintain in terraform/shell.

## Usage

```bash
# PVRE naws patch and report
aotutil ssm wait-patch i-1234456 --timeout 5m
aotutil ssm wait-patch-report i-1234456 --timeout 35m
```