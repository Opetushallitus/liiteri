'use strict'

const path = require('path')

module.exports = {
  apps: [
    {
      name: 'liiteri-16832',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['with-profile', 'dev', 'run'],
      cwd: __dirname,
      log_file: 'liiteri.log',
      pid_file: '.liiteri.pid',
      combine_logs: true,
      min_uptime: 30000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork'
    }
  ]
}
