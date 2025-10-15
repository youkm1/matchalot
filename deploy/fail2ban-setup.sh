#!/bin/bash

# Fail2ban installation and configuration script for Match-a-lot

echo "Installing Fail2ban..."
sudo apt-get update
sudo apt-get install -y fail2ban

echo "Creating custom filters..."

# Nginx security filter
sudo tee /etc/fail2ban/filter.d/nginx-matchalot.conf > /dev/null <<'EOF'
[Definition]
# Block suspicious requests
failregex = ^<HOST> - .* "(GET|POST|HEAD).*(\.git|\.env|wp-admin|phpmyadmin|\.\./).*" .*$
            ^<HOST> - .* ".*(\||union|select|insert|delete|update|drop|exec|script).*" .*$
            ^<HOST> - .* "(GET|POST|HEAD).*" (400|401|403|404|405|444) .*$
            ^<HOST> - .* ".*bot.*" 444 .*$

ignoreregex =

[Init]
datepattern = %%d/%%b/%%Y:%%H:%%M:%%S
EOF

# Spring authentication failures filter
sudo tee /etc/fail2ban/filter.d/spring-auth.conf > /dev/null <<'EOF'
[Definition]
failregex = .*\[<HOST>\].*인증 실패.*$
            .*\[<HOST>\].*접근 거부.*$
            .*\[<HOST>\].*OAuth2 인증에 실패했습니다.*$
            .*\[<HOST>\].*UNAUTHORIZED.*$
            .*\[<HOST>\].*FORBIDDEN.*$

ignoreregex =
EOF

# Rate limit filter
sudo tee /etc/fail2ban/filter.d/nginx-ratelimit.conf > /dev/null <<'EOF'
[Definition]
failregex = limiting requests, excess:.* by zone.*client: <HOST>
            limiting connections by zone.*client: <HOST>

ignoreregex =
EOF

echo "Creating jail configuration..."

# Jail configuration
sudo tee /etc/fail2ban/jail.local > /dev/null <<'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5
destemail = admin@match-a-lot.store
sender = fail2ban@match-a-lot.store
action = %(action_mwl)s

# Nginx suspicious requests
[nginx-matchalot]
enabled = true
port = http,https
filter = nginx-matchalot
logpath = /var/log/nginx/security.log
          /var/log/nginx/access.log
maxretry = 5
findtime = 600
bantime = 7200

# Spring authentication failures
[spring-auth]
enabled = true
port = http,https
filter = spring-auth
logpath = /app/logs/application.log
          /var/log/docker/backend.log
maxretry = 3
findtime = 300
bantime = 1800

# Nginx rate limiting
[nginx-ratelimit]
enabled = true
port = http,https
filter = nginx-ratelimit
logpath = /var/log/nginx/error.log
maxretry = 10
findtime = 60
bantime = 600

# SSH protection (always good to have)
[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
findtime = 600
bantime = 3600

# Nginx 4xx errors
[nginx-4xx]
enabled = true
port = http,https
filter = nginx-4xx
logpath = /var/log/nginx/access.log
          /var/log/nginx/security.log
maxretry = 30
findtime = 60
bantime = 600
EOF

echo "Starting Fail2ban service..."
sudo systemctl restart fail2ban
sudo systemctl enable fail2ban

echo "Fail2ban installation complete!"
echo ""
echo "Check status with:"
echo "  sudo fail2ban-client status"
echo "  sudo fail2ban-client status nginx-matchalot"
echo ""
echo "View banned IPs:"
echo "  sudo fail2ban-client status nginx-matchalot | grep 'Banned IP'"
echo ""
echo "Unban an IP:"
echo "  sudo fail2ban-client set nginx-matchalot unbanip <IP>"