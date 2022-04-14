package internal

type RunConfig struct {
	OutputLocation   string
	IncludedServices []string
	MaxBatches       int
}

func newDefaultRunConfig() RunConfig {
	defaultServices := []string{"EKS", "ECS", "EC2", "EKS-arm64", "EKS-operator", "EKS-fargate"}
	rc := RunConfig{
		IncludedServices: defaultServices,
		MaxBatches:       40,
	}
	return rc
}
