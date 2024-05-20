#Backup Dir
# Check if a value is provided as an argument, if not assign a default value
if [ $# -eq 0 ]; then
    backup_dir=/home/tihon/Desktop/basebackups
else
    backup_dir=$1
fi

host=localhost
usr=postgres
export PGPASSWORD="31313541" # TODO: use .pgpass instead? Read documentation
export PGPORT="5432"
echo -e "\n\nBackup Status: $(date +"%d-%m-%y")" >> $backup_dir/Status.log
echo -e "-----------------------" >> $backup_dir/Status.log
echo -e "\nStart Time: $(date)\n" >> $backup_dir/Status.log
/usr/lib/postgresql/12/bin/pg_basebackup -U $usr -h $host -w -D $backup_dir/PostgreSQL_Backup_$(date +"%d-%m-%y") -l "`date`" -P -F tar -z -R &>> $backup_dir/Status.log
echo -e "\nEnd Time: $(date)" >> $backup_dir/Status.log

#retention_duration=7
#find $backup_dir/PostgreSQL_Base_Backup* -type d -mtime +$retention_duration -exec rm -rv {} \;
