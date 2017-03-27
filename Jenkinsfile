pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                sh './bin/cibuild.sh create-uberjar'
            }
        }
    }
}
