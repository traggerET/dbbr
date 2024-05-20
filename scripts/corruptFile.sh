file=$1

systemctl stop postgresql

tail +10c $file > $file.truncated && cat "$file.truncated" > "$file" && rm "$file.truncated"

systemctl start postgresql
