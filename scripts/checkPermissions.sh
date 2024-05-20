cd /tmp
backup_folder=$(ls -t | grep PostgreSQL_Backup | head -n1)
backup_zip=$(ls ${backup_folder} | grep base)
mkdir unarchived
cat $(echo ${backup_zip}) | sudo tar xzf - -C /tmp/unarchived
for file in /tmp/unarchived/*
do
    # Check if the file is owned by "postgres"
    if [ "$(stat -c '%U' "$file")" != "postgres" ]; then
        echo "$file does not have owner 'postgres'"
        exit 1
    fi
    # Check if the permissions are set to 600 or 700
    permissions=$(stat -c '%a' "$file")
    if [ "$permissions" -ne 600 ] && [ "$permissions" -ne 700 ]; then
        echo "$file must have permissions set to 600 or 700."
        exit 1
    fi
done