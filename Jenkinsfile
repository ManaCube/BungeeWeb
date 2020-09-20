pipeline {
    agent any
    tools {
        maven 'Maven'
        jdk 'Java 8'
    }
    stages {
        stage ('Build') {
            steps {
                sh 'mvn install'
            }
        }
    }

    post {
        always {
            archiveArtifacts 'out/*.jar'
            script {
                pom = readMavenPom file: 'pom.xml'
                currentBuild.description = "v" + pom.version
            }
        }
    }
}