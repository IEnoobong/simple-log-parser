select * from log_entry where ip_address = @ip_address

select ip_address, count(ip_address) as number_of_requests from log_entry where log_time between @start_time and
@end_time group by ip_address having number_of_requests >= @request_count