package internal

import (
	"fmt"
	"log"
	"os"
)

type class struct {
	className string
	students  []student
}

type student struct {
	name   string
	rollNo int
	city   string
}

func main() {
	goerge := student{"Goerge", 35, "Newyork"}
	john := student{"Goerge", 25, "London"}

	students := []student{goerge, john}
	class := class{"firstA", students}

	fmt.Printf("class is %v\n", class)
	ghOutputFile := os.Setenv(fmt.Sprintf(`batch-keys=%s`, githubBatchKeys),"GITHUB_OUTPUT")

	arr := [4]string{"geek", "gfg", "Geeks1231", "GeeksforGeeks"}
	_, err = ghOutputFile.WriteString(fmt.Sprintf(`batch-values=%s`, arr))
	if err != nil {
		fmt.Printf("error writing string: %v", err)
	}
	_, err = ghOutputFile.WriteString("release-candidate-ready=success")
	if err != nil {
		fmt.Printf("error in setting GITHUB_OUTPUT env: %v", err)
	}
}
