
systemctl stop postgres.service

cp /var/lib/postgresql/12/main/pg_wal  ~/tmp_wal_storage
					        # может ьыть такое, что некоторые wal файлы будут незаархивированы wal-pip'ом.
					        # Эти файлы нужно  перенести в pg_wal после бекапа

rm -rf /var/lib/postgresql/12/main
mkdir /var/lib/postgresql/12/main
chown postgres:postgres /var/lib/postgresql/12/main
chmod 700 /var/lib/postgresql/12/main

# Look for backup folder, then find .tar.gz, and then cat it to tar
cd ~/Desktop/basebackups
backup_folder=$(ls -t | grep PostgreSQL_Backup | head -n1);
backup_zip=$(ls ${backup_folder} | grep base); 
cat $(echo ${backup_zip}) | sudo tar xzf - -C /var/lib/postgresql/12/main

rm -rf /var/lib/postgresql/12/main/pg_wal


#copy unarchived wal files
dir_A="$HOME/tmp_wal_storage"
dir_B="$HOME/Desktop/wal_archive"
dir_C="/var/lib/postgresql/12/main/pg_wal"

# Loop through files in directory A
for file in $dir_A/*; do
    # Extract filename from full path
    filename=$(basename "$file")
    
    # Check if the file exists in directory B
    if [ ! -e "$dir_B/$filename" ]; then
    	cp "$dir_A/$filename" "$dir_C"
    fi
done

restore_command="restore_command = 'cp ~/Desktop/wal_archive/%f %p'"
sed -i -e "s/.*restore_command.*/$restore_command/g" /etc/postgresql/12/main/postgresql.conf

touch /var/lib/postgresql/12/main/recovery.signal

systemctl start postgres.service

# loop and sleep until  recovery.signal not deleted
while [ -f /var/lib/postgresql/14/main/recovery.signal ]; do
    sleep 1
done

