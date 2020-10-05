# Run ECS

## run with the default config
``
cd ecs && terraform init && terraform apply
``

## run with the customized config
``
cd ecs && terraform init && terraform apply -var="ecs_taskdef_path=../template/ecstaskdef/default_ecs_taskdef.tpl" -var="otconfig_path=../template/otconfig/default_otconfig.tpl" -var="ecs_launch_type=FARGATE"
``
