
Basic usage:
------------

make start
	Starts all applications and docker containers

make stop
	Stop all applications and docker containers

make restart
	Restart all applications and docker containers

make test
	Run all tests

make lint
	Run linters

Advanced targets:
-----------------

make kill
	Kills pm2 and stops docker containers

make check-tools
	Verifies that all necessary tools are in path. Is called by most targets

make build-docker-images
	Build docker images using docker-compose. Is called by many other targets

make status
	Shows status of applications and docker containers

make log, make logs
	Follow logs via PM2

make clean
	Clean project. Removes unused docker containers and cleans compiled classes

Inspect makefile for details
