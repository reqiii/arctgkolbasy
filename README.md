# Manage Debts Telegram Bot

### Local run guide

1. Run DataBase:
    
    ```docker run --name ArctgKolbasyDB -e POSTGRES_PASSWORD=mysecretpassword -e POSTGRES_DB=arctgkolbasy_db -p 5432:5432 -d postgres```

2. Run migration
    
    ```gradle update -DPOSTGRES_USER=postgres -DPOSTGRES_PASSWORD=mysecretpassword -DPOSTGRES_URL=jdbc:postgresql://localhost:5432/arctgkolbasy_db```

3. Enjoy

   ```gradle bootRun```
