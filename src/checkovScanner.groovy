def call(Map config = [:]) {
    def directory = config.directory ?: '.'

    sh '''#!/bin/bash
        checkov -d ${directory} --compact --output json
    '''
}
return this
