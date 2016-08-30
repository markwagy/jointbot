groupstring <- function(data) {
  paste(
    data$grp_s1_s2,
    data$grp_s1_u1,
    data$grp_s1_u2,
    data$grp_s2_u3,
    data$grp_s2_u4,
    data$grp_u1_l1,
    data$grp_u2_l2,
    data$grp_u3_u3,
    data$grp_u4_l4,
    sep="")
}

## function to compute quantiles and return as data frame
quantiles.as.df <- function(df) {
  qq <- as.data.frame(as.list(quantile(df$distance, c(.05, .25, .50, .75, .95))))
  names(qq) <- paste('Q', c(5, 25, 50, 75, 95), sep='')
  qq
}
