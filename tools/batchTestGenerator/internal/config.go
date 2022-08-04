package internal

type RunConfig struct {
	OutputLocation   string
	IncludedServices map[string]struct{}
	MaxBatches       int
	TestCaseFilePath string
	EksARM64Vars     string
	EksVars          string
}

func NewDefaultRunConfig() RunConfig {
	defaultServices := []string{"EKS", "ECS", "EC2", "EKS_ARM64", "EKS_ADOT_OPERATOR", "EKS_ADOT_OPERATOR_ARM64", "EKS_FARGATE"}

	//build set for default services
	ism := make(map[string]struct{})
	for _, ds := range defaultServices {
		ism[ds] = struct{}{}
	}

	rc := RunConfig{
		IncludedServices: ism,
		MaxBatches:       40,
	}
	return rc
}
