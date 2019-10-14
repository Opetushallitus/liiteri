EXECUTABLES = lein docker docker-compose

LIITERI_CONFIG ?= ../ataru-secrets/virkailija-local-dev.edn

LIITERI=liiteri-16832

PM2=npx pm2 --no-autorestart
START_ONLY=start pm2.config.js --only
STOP_ONLY=stop pm2.config.js --only

DOCKER_COMPOSE=COMPOSE_PARALLEL_LIMIT=8 docker-compose

NODE_MODULES=node_modules/pm2/bin/pm2

# ----------------
# Check that all necessary tools are in path
# ----------------
check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

# ----------------
# Docker build
# ----------------
build-docker-images: check-tools
	$(DOCKER_COMPOSE) build

# ----------------
# Npm installation
# ----------------
$(NODE_MODULES):
	npm install

# ----------------
# Start apps
# ----------------
start-docker: build-docker-images
	$(DOCKER_COMPOSE) up -d

start-pm2: $(NODE_MODULES) start-docker
	$(PM2) start pm2.config.js

start-liiteri: start-docker
	$(PM2) $(START_ONLY) $(LIITERI)

# ----------------
# Stop apps
# ----------------
stop-pm2: $(NODE_MODULES)
	$(PM2) stop pm2.config.js

stop-liiteri:
	$(PM2) $(STOP_ONLY) $(LIITERI)

# ----------------
# Restart apps
# ----------------
restart-liiteri: start-liiteri

restart-docker: stop-docker start-docker

# ----------------
# Clean commands
# ----------------
clean-docker:
	docker-compose stop
	docker-compose rm
	docker system prune -f

clean-lein:
	lein clean

# ----------------
# Top-level commands (all apps)
# ----------------
start: start-pm2

stop: stop-pm2

restart: stop-pm2 start-pm2

clean: stop clean-lein clean-docker
	rm -f *.log
	rm -rf node_modules

status: $(NODE_MODULES)
	docker ps
	$(PM2) status

log: $(NODE_MODULES)
	$(PM2) logs

# Alias for log
logs: log

lint: $(NODE_MODULES)
	npx eslint .

help:
	@cat Makefile.md

# ----------------
# Test db management
# ----------------

test: start-docker
	lein test-local

# ----------------
# Kill PM2 and all apps managed by it (= everything)
# ----------------
kill: stop-pm2 stop-docker
	$(PM2) kill

