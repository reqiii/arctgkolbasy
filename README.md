# Manage Debts Telegram Bot

### Local run guide

1. Run DataBase:
    
    ```docker run --name ArctgKolbasyDB -e POSTGRES_PASSWORD=mysecretpassword -e POSTGRES_DB=arctgkolbasy_db --net=host -d postgres```

2. Run migration
    
    ```gradle update -DPOSTGRES_USER=postgres -DPOSTGRES_PASSWORD=mysecretpassword -DPOSTGRES_URL=jdbc:postgresql://localhost:5432/arctgkolbasy_db```

3. Build image

   ```DOCKER_BUILDKIT=1 docker build . -f Dockerfile -t arctgkolbasy:1```

4. Run image
   
   ```docker run --rm -d --name bot --net=host -e BOT_TOKEN=??? arctgkolbasy:1```

5. Do backups

   ```echo "0 22 * * * backup.sh TOKEN CHAT" | crontab -```
