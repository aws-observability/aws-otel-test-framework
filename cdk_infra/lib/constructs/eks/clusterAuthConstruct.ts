import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { Role } from 'aws-cdk-lib/aws-iam';

/**
 * Apply a set of opinionated ClusterRoles, ClusterRoleBindings
 * and AWSAuth map modifications. This construct does not create
 * new IAM roles but assumes that these roles already exist.
 */
export class ClusterAuth extends Construct {
  cluster: Cluster | FargateCluster;

  constructor(scope: Construct, id: string, props: ClusterAuthConstructProps) {
    super(scope, id);
    this.cluster = props.cluster;
    this.applyClusterAuthMap();
    this.applyClusterRbac();
  }
  applyClusterAuthMap() {
    const clusterReadOnlyRole = Role.fromRoleName(
      this,
      'clusterReadOnlyRole',
      'ClusterReadOnly'
    );
    this.cluster.awsAuth.addRoleMapping(clusterReadOnlyRole, {
      groups: ['adot-dev-readonly'],
      username: 'adot-readonly'
    });

    const clusterAdminRole = Role.fromRoleName(
      this,
      'clusterAdminRole',
      'ClusterAdmin'
    );
    this.cluster.awsAuth.addRoleMapping(clusterAdminRole, {
      groups: ['adot-dev-admin'],
      username: 'adot-admin'
    });

    const operatorRepoWorkflowRole = Role.fromRoleName(
      this,
      'operatorRepoWorkflowRole',
      'aws-obs-operator-gha'
    );
    this.cluster.awsAuth.addRoleMapping(operatorRepoWorkflowRole, {
      groups: ['adot-workflow-admin'],
      username: 'adot-operator-workflow'
    });

    const javaRepoWorkflowRole = Role.fromRoleName(
      this,
      'javaRepoWorkflowRole',
      'aws-obs-java-instrumentation-autoinstr-image-e2e-tests-gha'
    );
    this.cluster.awsAuth.addRoleMapping(javaRepoWorkflowRole, {
      groups: ['adot-workflow-admin'],
      username: 'adot-java-workflow'
    });

    const repoWorkflowRole = Role.fromRoleName(
      this,
      'repoWorkflowRole',
      'aws-obs-collector-gha'
    );
    this.cluster.awsAuth.addRoleMapping(repoWorkflowRole, {
      groups: ['adot-workflow-admin'],
      username: 'adot-workflow'
    });
  }
  applyClusterRbac() {
    const roClusterRole = {
      apiVersion: 'rbac.authorization.k8s.io/v1',
      kind: 'ClusterRole',
      metadata: {
        name: 'otel-operator-readonly',
        labels: {
          'rbac.authorization.k8s.io/aggregate-to-view': 'true'
        }
      },
      rules: [
        {
          apiGroups: ['opentelemetry.io'],
          resources: ['instrumentations', 'opentelemetrycollectors'],
          verbs: ['get', 'list', 'watch']
        },
        {
          apiGroups: ['admissionregistration.k8s.io'],
          resources: ['*'],
          verbs: ['get', 'list', 'watch']
        },
        {
          apiGroups: [''],
          resources: ['nodes'],
          verbs: ['get', 'list', 'watch']
        }
      ]
    };

    const roClusterRoleBinding = {
      kind: 'ClusterRoleBinding',
      apiVersion: 'rbac.authorization.k8s.io/v1',
      metadata: { name: 'adot-dev-readonly-crb' },
      subjects: [
        {
          kind: 'Group',
          name: 'adot-dev-readonly',
          apiGroup: 'rbac.authorization.k8s.io'
        }
      ],
      roleRef: {
        kind: 'ClusterRole',
        name: 'view',
        apiGroup: 'rbac.authorization.k8s.io'
      }
    };

    const adminClusterRoleBinding = {
      kind: 'ClusterRoleBinding',
      apiVersion: 'rbac.authorization.k8s.io/v1',
      metadata: { name: 'adot-dev-admin-crb' },
      subjects: [
        {
          kind: 'Group',
          name: 'adot-dev-admin',
          apiGroup: 'rbac.authorization.k8s.io'
        }
      ],
      roleRef: {
        kind: 'ClusterRole',
        name: 'cluster-admin',
        apiGroup: 'rbac.authorization.k8s.io'
      }
    };

    const workflowClusterRoleBinding = {
      kind: 'ClusterRoleBinding',
      apiVersion: 'rbac.authorization.k8s.io/v1',
      metadata: { name: 'adot-workflow-admin-crb' },

      subjects: [
        {
          kind: 'Group',
          name: 'adot-workflow-admin',
          apiGroup: 'rbac.authorization.k8s.io'
        }
      ],
      roleRef: {
        kind: 'ClusterRole',
        name: 'cluster-admin',
        apiGroup: 'rbac.authorization.k8s.io'
      }
    };
    this.cluster.addManifest(
      'ClusterRoleBindings',
      roClusterRole,
      roClusterRoleBinding,
      adminClusterRoleBinding,
      workflowClusterRoleBinding
    );
  }
}

export interface ClusterAuthConstructProps {
  cluster: Cluster | FargateCluster;
}
