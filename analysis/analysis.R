library(RMySQL)
library(ggplot2)
library(gridExtra)
library(plyr)
library(reshape)

source("funcs.R")

data.from.db <- function() {
  mydb = dbConnect(MySQL(), user='webuser', password='webber', dbname='jointbot', host='patton')
  rs <- dbSendQuery(mydb, 'select a.*, b.is_control from tbl_configs a join tbl_usergroup b on a.ip_address = b.ip_address')
  data = fetch(rs, n=-1)
  data <- cbind(data,groupstring = groupstring(data))

  ## use time class for timestamp
  data$ts <- as.POSIXlt(strptime(data$ts,format="%Y-%m-%d %H:%M:%S"))
  return(data)
}

get.data.simp <- function(data) {
  keeper.fields <-
    c("id","ts","distance","background_run",
      "ip_address","is_control","groupstring")
  data.simp <- data[,keeper.fields]
  data.simp$is_control <- data.simp$is_control==1
  return(data.simp)
}

plot.sequential.dists <- function(data.simp) {
  filter.win = 50
  p1 <- ggplot(data.simp,aes(id,distance,col=is_control)) +
    geom_point() +
      stat_smooth(n=filter.win) +
        ggtitle("distances achieved by control/experimental groups in sequence")
  p2 <- ggplot(data.simp,aes(ts,distance,col=is_control)) +
    geom_point() +
      stat_smooth(n=filter.win) +
        ggtitle("distances achieved by control/experimental groups in time")
  grid.arrange(p1,p2)
}


quantile.ribbon.plot <- function(dd, color='blue') {
  ## Function to compute quantiles and return a data frame
  g <- function(d) {
    qq <- as.data.frame(as.list(quantile(d$distance, c(.05, .25, .50, .75, .95))))
    names(qq) <- paste('Q', c(5, 25, 50, 75, 95), sep = '')
    qq   }

  ## Apply function to each year of data in dd:
  qdf <- ddply(dd, .(window), g)
  ## melt to produce a factor variable whose levels are quantiles
  qdfm <- melt(qdf, id = 'window')

  ## Use ggplot() to plot the boxplots and quantile lines:
  ## ggplot() +
  ##   geom_boxplot(data = dd, aes(x = factor(window), y = distance)) +
  ##     geom_line(data = qdfm, aes(x = factor(window), y = value,
  ##                 group = variable, colour = variable),
  ##               size = 1) +
  ##                 labs(x = 'Window', colour = 'Quantile')

  p <- ggplot(qdf, aes(x = window, y = Q50)) +
    geom_line(size = 2, color = 'black') +
    geom_ribbon(aes(ymin = Q25, ymax = Q75), fill = 'blue', alpha = 0.4) +
    geom_ribbon(aes(ymin = Q5, ymax = Q25), fill = 'blue', alpha = 0.2) +
    geom_ribbon(aes(ymin = Q75, ymax = Q95), fill = 'blue', alpha = 0.2) +
    labs(x = 'Window', y = 'Distance')
  return(p)
}


quantile.ribbon.plot.exp.ctl <- function(dd.exp, dd.ctl) {
  alpha=0.3
  line.type=3
  
  ## Function to compute quantiles and return a data frame
  g <- function(d) {
    qq <- as.data.frame(as.list(quantile(d$distance, c(.05, .25, .50, .75, .95))))
    names(qq) <- paste('Q', c(5, 25, 50, 75, 95), sep = '')
    qq   }

  ## Apply function to each year of data in dd:
  qdf.exp <- ddply(dd.exp, .(window), g)
  qdf.ctl <- ddply(dd.ctl, .(window), g)
  ## melt to produce a factor variable whose levels are quantiles
  qdfm.exp <- melt(qdf.exp, id = 'window')
  qdfm.ctl <- melt(qdf.ctl, id = 'window')

  qdf <- cbind(rbind(qdf.exp, qdf.ctl),group=c(rep("experimental",nrow(qdf.exp)),rep("control",nrow(qdf.ctl))))

  p <- ggplot(qdf, aes(x = window, y = Q50, col=group, fill=group)) +
    geom_line(size = 2) +
    geom_ribbon(aes(ymin = Q25, ymax = Q75), alpha = alpha, linetype=line.type) +
    geom_ribbon(aes(ymin = Q5, ymax = Q25), alpha = alpha, linetype=line.type) +
    geom_ribbon(aes(ymin = Q75, ymax = Q95), alpha = alpha, linetype=line.type) +
    labs(x = 'Window', y = 'Distance')
  return(p)
}

plot.median.smooth <- function(data.simp, window.size=20) {

  ## plot strategy is using this post:
  ## https://stat.ethz.ch/pipermail/r-help/2011-October/291575.html

  ## attach windowing vector to each of experimental and control groups
  ## exp
  tmp.exp <- with(data.simp[!data.simp$is_control,], data.frame(distance=distance))
  window <- rep(1:ceiling(nrow(tmp.exp)/window.size),each = window.size, length.out = nrow(tmp.exp))
  tmp.exp <- cbind(tmp.exp, window)
  ## ctl
  tmp.ctl <- with(data.simp[ data.simp$is_control,], data.frame(distance=distance))
  window <- rep(1:ceiling(nrow(tmp.ctl)/window.size),each = window.size, length.out = nrow(tmp.ctl))
  tmp.ctl <- cbind(tmp.ctl, window)

  ## --------------------
  # get quantile ribbon plots for exp and control
  p.exp <- quantile.ribbon.plot(tmp.exp) + ggtitle("experimental")
  p.ctl <- quantile.ribbon.plot(tmp.ctl,'red') + ggtitle("control")
##  grid.arrange(p.exp, p.ctl)
  p <- quantile.ribbon.plot.exp.ctl(tmp.exp, tmp.ctl)
  p <- p + ggtitle("plot of quantile distances of control and experimental groups")
  p
  ## --------------------
        
}

plot.distance.dists <- function(data.simp) {
  ggplot(data.simp,aes(ip_address,distance,fill=ip_address))+ geom_boxplot() +
    facet_grid(is_control~.) +
      ggtitle("control versus experimental distance distributions")

  ggplot(data.simp,aes(ip_address,distance,fill=ip_address)) +
    geom_boxplot() +
      facet_grid(is_control~.) +
        ggtitle("control versus experimental distance distributions \n (facet label indicates whether user is in control group)") +
          theme(legend.position="none", axis.text.x=element_blank()) +
            xlab("user")

  p3 <- ggplot(data.simp,aes(distance,fill=is_control)) + geom_density(alpha=0.3) + ggtitle("overall distance comparison between control and experimental")
  p4 <- ggplot(data.simp,aes(distance,fill=is_control)) + geom_histogram(position="dodge")
  grid.arrange(p3,p4)
}

unique.users.per.group <- function(data.simp) {
  ## get unique users per control/experimental group
  tmp <- data.simp[,c("ip_address","is_control")]
  tmp <- unique(tmp)
  p5 <- ggplot(data.simp,aes(is_control,fill=is_control)) +
    geom_histogram(alpha=0.5) +
      ggtitle("number of control versus experimental runs") +
        stat_bin(geom="text", aes(label=..count.., vjust=2)) +
          theme(legend.position="none")
  p6 <- ggplot(tmp,aes(is_control,fill=is_control)) +
    geom_histogram(alpha=0.5) +
      ggtitle("number of control versus experimental users") +
        stat_bin(geom="text", aes(label=..count.., vjust=2)) +
          theme(legend.position="none")
  grid.arrange(p5,p6)
}

## for a sliding window of number of users to average (faceted on experimental vs control group)
## in the beginning of the experiment and the end of the experiment, get mean and stdev of distances
## stop when we are at the min of the total number of experimental or control users

separate.data.groups <- function(data.simp) {
  data.ctl <- data.simp[data.simp$is_control==TRUE, ]
  data.exp <- data.simp[data.simp$is_control==FALSE,]
  return(list(ctl=data.ctl,exp=data.exp))
}


##run.windowed(data.ctl, data.exp)
## backwards means that we are running windows starting from the last run and expanding toward the last first run
## (backwards was switched from previous defn to make clearer)
run.windowed <- function(data.ctl, data.exp, backwards=FALSE, incr.val=20) {

  max.winsize <- min(nrow(data.ctl),nrow(data.exp))
  len.ctl <- length(win.indic.ctl)
  len.exp <- length(win.indic.exp)

  winsize.vals <- seq(incr.val, max.winsize, by=incr.val)

  ## initialize return data structure
  rtn.data <- data.frame(
    num.runs =        rep(NA, length(winsize.vals)),
    num.users.ctl =   rep(NA, length(winsize.vals)),
    num.users.exp =   rep(NA, length(winsize.vals)),
    num.designs.ctl = rep(NA, length(winsize.vals)),
    num.designs.exp = rep(NA, length(winsize.vals)),
    wilcox.p.values = rep(NA, length(winsize.vals)),
    ttest.p.values =  rep(NA, length(winsize.vals)),
    mean.ctl =        rep(NA, length(winsize.vals)),
    mean.exp =        rep(NA, length(winsize.vals)),
    median.ctl =      rep(NA, length(winsize.vals)),
    median.exp =      rep(NA, length(winsize.vals)),
    min.ctl =         rep(NA, length(winsize.vals)),
    min.exp =         rep(NA, length(winsize.vals)),
    max.ctl =         rep(NA, length(winsize.vals)),
    max.exp =         rep(NA, length(winsize.vals)))

  i = 0
  for (winsize in winsize.vals) {
    i=i+1
    rtn.data$num.runs[i]=winsize

    ## indicator vectors of which vals to include
    win.indic.ctl <- rep(FALSE, nrow(data.ctl))
    win.indic.exp <- rep(FALSE, nrow(data.exp))
    
    ttl <- paste("window size:", winsize)
    print(ttl)

    file.prefix = ""
    ## calculate window on which to operate
    if (!backwards) {
      # front to back
      start.idx.ctl = 1
      start.idx.exp = 1
      end.idx.ctl = winsize
      end.idx.exp = winsize
      file.prefix = "begin"
    } else {
      # back to front
      start.idx.ctl=(len.ctl-winsize)+1
      start.idx.exp=(len.exp-winsize)+1
      end.idx.ctl = len.ctl
      end.idx.exp = len.exp
      file.prefix = "end"
    }
    win.indic.ctl[start.idx.ctl:end.idx.ctl] = TRUE
    win.indic.exp[start.idx.exp:end.idx.exp] = TRUE

    ## windowed control and experimental data
    df.ctl.windowed <- data.ctl[win.indic.ctl,]
    df.exp.windowed <- data.exp[win.indic.exp,]
    df.both.windowed <- rbind(df.ctl.windowed, df.exp.windowed)

    ## do a shapiro test for normality
    swtst.ctl <- shapiro.test(df.ctl.windowed$distance)
    swtst.exp <- shapiro.test(df.exp.windowed$distance)

    ## do the t test
    ttst <- with(df.both.windowed,  t.test(distance~is_control))
    rtn.data$ttest.p.values[i] = ttst$p.value

    ## wilcoxon rank test (non-parametric)
    wrtst <- with(df.both.windowed, wilcox.test(distance~is_control))
    rtn.data$wilcox.p.values[i] = ttst$p.value
    
    ## count the number of users in both control and exp groups
    agg.ctl.users <- with(df.ctl.windowed, aggregate(rep(1,nrow(df.ctl.windowed)), by=list(ip_address), sum))
    agg.exp.users <- with(df.exp.windowed, aggregate(rep(1,nrow(df.exp.windowed)), by=list(ip_address), sum))
    rtn.data$num.users.ctl[i] = nrow(agg.ctl.users)
    rtn.data$num.users.exp[i] = nrow(agg.exp.users)

    ## count the number of designs in both control and exp groups
    agg.ctl.designs <- with(df.ctl.windowed, aggregate(rep(1,nrow(df.ctl.windowed)), by=list(groupstring), sum))
    agg.exp.designs <- with(df.exp.windowed, aggregate(rep(1,nrow(df.exp.windowed)), by=list(groupstring), sum))
    rtn.data$num.designs.ctl[i] = nrow(agg.ctl.designs)
    rtn.data$num.designs.exp[i] = nrow(agg.exp.designs)

    ## write designs to file
    write.csv(agg.ctl.designs, sprintf("data/designs_%s_winsize_%d_ctl.csv",file.prefix, winsize))
    write.csv(agg.exp.designs, sprintf("data/designs_%s_winsize_%d_exp.csv",file.prefix, winsize))
              
    pdf(sprintf("figs/%s_winsize_%d.pdf",file.prefix, winsize))

    ## how much to round floating point numbers
    round.amt = 5

    ## calculate various distribution summary quantities
    rtn.data$mean.ctl[i]   = mean(df.ctl.windowed$distance)
    rtn.data$mean.exp[i]   = mean(df.exp.windowed$distance)
    rtn.data$median.ctl[i] = median(df.ctl.windowed$distance)
    rtn.data$median.exp[i] = median(df.exp.windowed$distance)
    rtn.data$min.ctl[i]    = min(df.ctl.windowed$distance)
    rtn.data$min.exp[i]    = min(df.exp.windowed$distance)
    rtn.data$max.ctl[i]    = max(df.ctl.windowed$distance)
    rtn.data$max.exp[i]    = max(df.exp.windowed$distance)

    distinfo.str <- paste(
      "mean:", mean(df.ctl.windowed$distance),"(ctl), ", mean(df.exp.windowed$distance),"(exp)",
      "\nmedian:", median(df.ctl.windowed$distance),"(ctl), ", median(df.exp.windowed$distance),"(exp)",
      "\nrange:","[", min(df.ctl.windowed$distance), "-", max(df.ctl.windowed$distance), "] (ctl), ",
      "[", min(df.exp.windowed$distance), "-", max(df.exp.windowed$distance), "] (exp)")
    ttest.str <- paste("t-test p-value:",round(ttst$p.value,round.amt))
    wilcox.str <- paste("Wilcoxon rank sum test p-value:", round(wrtst$p.value,round.amt))
    shapiro.str <- paste("Shapiro-Wilks normality p-values:\n",
                         round(swtst.ctl$p.value,round.amt),"(control), ",
                         round(swtst.exp$p.value,round.amt),"(experimental)")
    nusers.str <- paste("users in control group:",nrow(agg.ctl.users),
                        ", users in experimental group:",nrow(agg.exp.users))
    ndesigns.str <- paste("designs in control group:",nrow(agg.ctl.designs),
                          ", designs in experimental group:",nrow(agg.exp.designs))

    p1 <- ggplot(df.both.windowed,aes(is_control,distance,fill=is_control)) +
      geom_boxplot() +
        theme(legend.position="none") +
          ggtitle(paste(ttl, distinfo.str, ttest.str, wilcox.str, shapiro.str, nusers.str, ndesigns.str, sep="\n"))
    p2 <- ggplot(df.both.windowed,aes(distance,fill=is_control)) + geom_density(alpha=0.5)
##    p3 <- ggplot(df.both.windowed,aes(distance,fill=is_control)) + geom_density(alpha=0.5) + scale_x_log10()
    p <- grid.arrange(p1,p2)
    print(p)
    dev.off()
    pdf(sprintf("figs/qq_%s_winsize_%d.pdf",file.prefix, winsize))
    par(mfrow=c(2,1))
    qqnorm(df.ctl.windowed$distance)
    qqnorm(df.exp.windowed$distance)
    dev.off()
  }
  return(rtn.data)
}  


get.user.num.vector <- function(data.simp) {
  ips <- data.simp$ip_address
  curr.user.id=0
  prev.ip = ""
  ids <- rep(NA,length(ips))
  for (i in seq(1,length(ips))) {
    if (ips[i]!=prev.ip) {
      curr.user.id=curr.user.id+1
      prev.ip = ips[i]
    }
    ids[i] = curr.user.id
  }
  return(ids)
}


plot.user.distances <- function(data.ctl, data.exp) {
  user.id.exp=get.user.num.vector(data.exp)
  user.id.ctl=get.user.num.vector(data.ctl)
  agg.ctl <- aggregate(data.ctl$distance,by=list(user.id.ctl),mean)
  agg.exp <- aggregate(data.exp$distance,by=list(user.id.exp),mean)
  df.ctl <- data.frame(user.num=agg.ctl$Group.1,distance=agg.ctl$x,group="control")
  df.exp <- data.frame(user.num=agg.exp$Group.1,distance=agg.exp$x,group="experimental")
  df.both <- rbind(df.ctl,df.exp)
  pdf("figs/distance_over_usernums.pdf")
  ggplot(df.both,aes(user.num, distance,col=group)) + geom_line() + geom_smooth()
  dev.off()
}


main <- function() {
  ## from end to begin with run sample increments of 10
  win.10.end <- run.windowed(data.ctl, data.exp, TRUE, 10)
  ## from begin towards end with run sample increments of 10
  win.10.begin <- run.windowed(data.ctl, data.exp, FALSE, 10)

  ## wilcox p value decrease of control vs experimental populations as time progresses
  p.wilcox.begin <- ggplot(win.10.begin, aes(num.runs, wilcox.p.values)) +
    geom_line() +
      geom_point() +
      geom_abline(slope=0, intercept=0.05, col="red", linetype=2) +
##        annotate(text="\alpha=0.05 significance value", x=10, y=0.1, col="red") + 
          ggtitle(
            expression(
              paste("Wilcoxon Rank Sum Test p-values. ",
                    "\nSampling 'window size' runs from ",
                    bold(beginning),
                    " of trial"))) +
                      xlim(1,200) +
                        xlab("window size (in runs)")
  
  p.wilcox.end <- ggplot(win.10.end, aes(num.runs, wilcox.p.values)) +
    geom_line() +
      geom_point() +
      geom_abline(slope=0, intercept=0.05, col="red", linetype=2) +
          ggtitle(
            expression(
              paste("Wilcoxon Rank Sum Test p-values. ",
                    "\nSampling 'window-size' runs from ",
                    bold(end),
                    " of trial"))) +
                      xlim(1,200) +
              xlab("window size (in runs)")

  grid.arrange(p.wilcox.begin, p.wilcox.end)

  ## ## plot mean distances as window increases backwards
  ## tmp.means <- with(win.10.end,
  ##                   data.frame(
  ##                     mean=c(mean.ctl, mean.exp),
  ##                     group=c(
  ##                       rep("control",length(mean.ctl)),
  ##                       rep("experimental",length(mean.exp)))))

  ## tmp.medians <- with(win.10.end,
  ##                   data.frame(
  ##                     median=c(median.ctl,median.exp),
  ##                     group=c(
  ##                       rep("control",length(mean.ctl)),
  ##                       rep("experimental",length(mean.exp)))))

  ## plot.sequential.dists(data.simp)
  plot.median.smooth(data.simp,20)
}
